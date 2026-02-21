package com.project.analyzer.leak.impl

import com.project.analyzer.api.di.IO
import com.project.analyzer.leak.api.LeakCanaryController
import com.project.analyzer.utils.AppDirectories
import com.project.analyzer.utils.logger.logger
import com.sun.management.HotSpotDiagnosticMXBean
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import leakcanary.GcTrigger
import leakcanary.KeyedWeakReference
import leakcanary.ReferenceQueueRetainedObjectTracker
import leakcanary.inProcess
import shark.HeapAnalysis
import shark.HeapAnalysisFailure
import shark.HeapAnalysisSuccess
import shark.HeapAnalyzer
import shark.HprofHeapGraph.Companion.openHeapGraph
import shark.KeyedWeakReferenceFinder
import shark.MetadataExtractor
import shark.ObjectInspectors
import shark.OnAnalysisProgressListener
import shark.SharkLog
import java.io.File
import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyyMMdd_HHmmss")
    .withZone(ZoneId.systemDefault())

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<LeakCanaryController>())
class LeakCanaryControllerImpl(
    private val directories: AppDirectories,
    @param:IO private val ioDispatcher: CoroutineDispatcher,
) : LeakCanaryController {

    private val logger = logger()
    private val config = LeakCanaryConfig.fromSystemProperties()
    private val started = AtomicBoolean(false)
    private val analysisInProgress = AtomicBoolean(false)
    private val lastAnalysisUptimeMs = AtomicLong(0)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val clock = UptimeClock

    @Volatile
    private var retainedObjectTracker: ReferenceQueueRetainedObjectTracker? = null

    private val objectWatcherMutex = Mutex()

    private val watcherLock = Any()

    override val isEnabled: Boolean get() = config.enabled

    override suspend fun start() {
        if (!config.enabled) {
            logger.info { "LeakCanary disabled." }
            return
        }
        if (!started.compareAndSet(false, true)) return
        ensureWatcher()
        SharkLog.logger = SharkLogger
        cleanupOldDumps()
        logger.info {
            "LeakCanary started (delay=${config.watchDelayMillis}ms, cooldown=${config.analysisCooldownMillis}ms)."
        }
    }

    override suspend fun stop() {
        if (!started.compareAndSet(true, false)) return

        scope.coroutineContext.cancelChildren()

        retainedObjectTracker = null

        logger.info { "LeakCanary stopped." }
    }

    override fun watch(watchedObject: Any, description: String) {
        if (!config.enabled || !started.get()) return
        val tracker = retainedObjectTracker ?: return

        tracker.expectDeletionOnTriggerFor(watchedObject, description)
    }

    override fun dumpNow(reason: String?) {
        if (!config.enabled || !started.get()) return
        scheduleAnalysis(reason ?: "manual dump")
    }

    private suspend fun ensureWatcher() {
        retainedObjectTracker ?: objectWatcherMutex.withLock(watcherLock) {
            if (retainedObjectTracker != null) return

            retainedObjectTracker = ReferenceQueueRetainedObjectTracker(
                clock = clock,
                onObjectRetainedListener = {}
            )
        }
    }

    private fun scheduleAnalysis(reason: String) {
        if (!config.enabled || !started.get()) return

        val now = clock.uptime().inWholeMilliseconds
        val last = lastAnalysisUptimeMs.get()

        if (now - last < config.analysisCooldownMillis) return
        if (!analysisInProgress.compareAndSet(false, true)) return

        lastAnalysisUptimeMs.set(now)

        scope.launch {
            try {
                analyzeRetainedObjects(reason)
            } catch (t: Throwable) {
                logger.error(t) { "LeakCanary analysis failed." }
            } finally {
                analysisInProgress.set(false)
            }
        }
    }

    private suspend fun analyzeRetainedObjects(reason: String) {
        val tracker = retainedObjectTracker ?: return
        val retainedCount = tracker.retainedObjectCount
        if (retainedCount == 0) return

        GcTrigger.inProcess().runGc()
        if (tracker.retainedObjectCount == 0) return

        val heapDumpFile = newHeapDumpFile()
        KeyedWeakReference.heapDumpUptimeMillis = clock.uptime().inWholeMilliseconds
        if (!dumpHeap(heapDumpFile)) return

        val analyzer = HeapAnalyzer(OnAnalysisProgressListener.NO_OP)

        val analysis = withContext(ioDispatcher) {
            heapDumpFile.openHeapGraph().use { graph ->
                analyzer.analyze(
                    heapDumpFile = heapDumpFile,
                    graph = graph,
                    leakingObjectFinder = KeyedWeakReferenceFinder,
                    computeRetainedHeapSize = true,
                    objectInspectors = ObjectInspectors.jdkDefaults,
                    metadataExtractor = MetadataExtractor.NO_OP,
                )
            }
        }

        handleAnalysis(analysis, heapDumpFile, reason)
        cleanupOldDumps()
    }

    private fun dumpHeap(heapDumpFile: File): Boolean {
        heapDumpFile.parentFile?.mkdirs()
        val mxBean = runCatching {
            ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean::class.java)
        }.getOrNull()

        if (mxBean == null) {
            logger.warn { "LeakCanary heap dump skipped: HotSpotDiagnosticMXBean unavailable." }
            return false
        }

        return runCatching {
            mxBean.dumpHeap(heapDumpFile.absolutePath, true)
            true
        }.getOrElse { error ->
            logger.error(error) { "LeakCanary heap dump failed: ${heapDumpFile.absolutePath}" }
            false
        }
    }

    private fun handleAnalysis(analysis: HeapAnalysis, heapDumpFile: File, reason: String) {
        val reportFile = writeReport(heapDumpFile, analysis, reason)
        when (analysis) {
            is HeapAnalysisSuccess -> {
                val leakCount = analysis.applicationLeaks.size + analysis.libraryLeaks.size
                if (leakCount == 0) {
                    logger.info { "LeakCanary: no leaks found (${heapDumpFile.name})." }
                    if (!config.retainHeapDumpOnNoLeaks) {
                        heapDumpFile.delete()
                        reportFile?.delete()
                    }
                } else {
                    logger.warn {
                        "LeakCanary: $leakCount leak(s) found. Heap dump: ${heapDumpFile.absolutePath}"
                    }
                    if (reportFile != null) {
                        logger.warn { "LeakCanary report: ${reportFile.absolutePath}" }
                    }
                    analysis.applicationLeaks.forEachIndexed { index, leak ->
                        logger.warn {
                            "Leak #${index + 1}: ${leak.shortDescription} " +
                                "(retained=${leak.totalRetainedHeapByteSize ?: -1} bytes)"
                        }
                    }
                    analysis.libraryLeaks.forEachIndexed { index, leak ->
                        logger.warn {
                            "Library leak #${index + 1}: ${leak.shortDescription} " +
                                "(retained=${leak.totalRetainedHeapByteSize ?: -1} bytes)"
                        }
                    }
                }
            }

            is HeapAnalysisFailure -> {
                logger.error(analysis.exception) {
                    "LeakCanary: analysis failed for ${heapDumpFile.absolutePath}"
                }
            }
        }
    }

    private fun writeReport(heapDumpFile: File, analysis: HeapAnalysis, reason: String): File? {
        val reportFile = File(heapDumpFile.parentFile, "${heapDumpFile.nameWithoutExtension}.analysis.txt")

        return runCatching {
            reportFile.writeText(
                buildString {
                    appendLine("Reason: $reason")
                    appendLine()
                    appendLine(analysis.toString())
                },
            )
            reportFile
        }.getOrNull()
    }

    private fun cleanupOldDumps() {
        val dumpDir = dumpDirectory()
        val dumps = dumpDir.listFiles { file -> file.extension.equals("hprof", ignoreCase = true) }
            ?.sortedBy { it.lastModified() }
            ?: return

        if (dumps.size <= config.maxStoredHeapDumps) return
        val toDelete = dumps.take(dumps.size - config.maxStoredHeapDumps)

        toDelete.forEach { dump ->
            dump.delete()
            File(dumpDir, "${dump.nameWithoutExtension}.analysis.txt").delete()
        }
    }

    private fun dumpDirectory(): File =
        File(directories.cacheDir, config.dumpDirectoryName)
            .apply { mkdirs() }

    private fun newHeapDumpFile(): File {
        val timestamp = TIMESTAMP_FORMAT.format(Instant.now())
        return File(dumpDirectory(), "leak_$timestamp.hprof")
    }
}

private object SharkLogger : SharkLog.Logger {

    private val logger = logger()

    override fun d(message: String) {
        logger.debug { message }
    }

    override fun d(throwable: Throwable, message: String) {
        logger.debug(throwable) { message }
    }
}

private object WatcherThreadFactory : ThreadFactory {

    override fun newThread(runnable: Runnable): Thread = Thread(runnable, "LeakCanary-Watcher").apply {
        isDaemon = true
        priority = Thread.NORM_PRIORITY
    }
}
