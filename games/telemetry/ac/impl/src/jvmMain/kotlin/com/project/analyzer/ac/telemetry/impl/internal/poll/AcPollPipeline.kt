package com.project.analyzer.ac.telemetry.impl.internal.poll

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.fallback.AcFallbackPatchMemory
import com.project.analyzer.ac.telemetry.impl.internal.poll.pipeline.AcFallbackPollSnapshotAdapter
import com.project.analyzer.ac.telemetry.impl.internal.poll.pipeline.AcPollSnapshotAdapter
import com.project.analyzer.ac.telemetry.impl.internal.poll.pipeline.AcPollSnapshotAdapterContext
import com.project.analyzer.ac.telemetry.impl.internal.poll.pipeline.AcPollSnapshotPipeline
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcLegacyRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcPollSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStaticPageView
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Inject
internal class AcPollPipeline(private val pollLoop: AcPollLoop, snapshotAdapters: Set<AcPollSnapshotAdapter>) {

    private val logger = logger()
    private val physicsSize = SPageFilePhysics().size()
    private val graphicsSize = SPageFileGraphics().size()
    private val staticsSize = SPageFileStatic().size()

    private val legacyPhysicsBuffer = ByteArray(physicsSize)
    private val legacyGraphicsBuffer = ByteArray(graphicsSize)
    private val legacyStaticsBuffer = ByteArray(staticsSize)
    private val acePhysicsBuffer = ByteArray(physicsSize)
    private val aceGraphicsBuffer = ByteArray(AcEvoGraphicsPageView.SIZE_BYTES)
    private val aceStaticsBuffer = ByteArray(AcEvoStaticPageView.SIZE_BYTES)

    private val legacySnapshotPool = ConcurrentLinkedDeque<AcLegacyRawSnapshot>()
    private val aceSnapshotPool = ConcurrentLinkedDeque<AceRawSnapshot>()

    private var boundAceSnapshot: AceRawSnapshot? = null
    private val fallbackMemory = object : AcFallbackPatchMemory {
        override val physics: SPageFilePhysics
            get() = requireBoundAceSnapshot().physics

        override val graphics: SPageFileGraphics
            get() = requireBoundAceSnapshot().fallback.graphics

        override val statics: SPageFileStatic
            get() = requireBoundAceSnapshot().fallback.statics
    }

    private var pollDispatcher: ExecutorCoroutineDispatcher? = null
    private var pollJob: Job? = null
    private var channel: Channel<PollResult>? = null
    private var currentState: GameConnectionState = GameConnectionState.DISCONNECTED
    private var useDedicatedPollThread: Boolean = true
    private var useSnapshotPool: Boolean = true
    private val snapshotPipeline = AcPollSnapshotPipeline(adapters = snapshotAdapters.toList())
    private val snapshotAdapterContext = SnapshotAdapterContext()

    internal constructor(
        pollLoop: AcPollLoop,
        fallback: AcEvoFallbackShmPatcher,
        useDedicatedPollThread: Boolean,
    ) : this(
        pollLoop = pollLoop,
        snapshotAdapters = setOf(AcFallbackPollSnapshotAdapter(fallback)),
    ) {
        this.useDedicatedPollThread = useDedicatedPollThread
    }

    init {
        repeat(SNAPSHOT_POOL_SIZE) {
            legacySnapshotPool.addLast(newLegacySnapshot())
            aceSnapshotPool.addLast(newAceSnapshot())
        }
    }

    fun start(scope: CoroutineScope): Channel<PollResult> {
        channel?.let { return it }

        val created = Channel<PollResult>(capacity = SNAPSHOT_POOL_SIZE + STATE_BUFFER_CAPACITY)
        channel = created

        pollJob = scope.launch(pollLaunchContext()) {
            pollLoop.start { result ->
                handlePollResult(result, created)
            }
        }

        return created
    }

    suspend fun stop() {
        pollJob?.cancelAndJoin()
        pollJob = null

        channel?.let { pending ->
            drainPendingSnapshots(pending)
            pending.close()
        }
        channel = null

        closePollDispatcher()
        snapshotPipeline.onStop()
        pollLoop.stop()
    }

    fun release(snapshot: AcPollSnapshot) {
        if (!useSnapshotPool) return
        releaseToPool(snapshot)
    }

    private fun handlePollResult(result: PollResult, channel: Channel<PollResult>) {
        when (result) {
            is PollResult.StateChanged -> handleStateChanged(result, channel)
            is PollResult.Frame -> handleFrame(result.snapshot, channel)
        }
    }

    private fun handleStateChanged(result: PollResult.StateChanged, channel: Channel<PollResult>) {
        currentState = result.state
        snapshotPipeline.onStateChanged(result.state)
        if (!channel.trySend(result).isSuccess) {
            logDrop("state_change")
        }
    }

    private fun handleFrame(source: AcPollSnapshot, channel: Channel<PollResult>) {
        if (!useSnapshotPool) {
            if (!channel.trySend(PollResult.Frame(source)).isSuccess) {
                logDrop("queue_full")
            }
            return
        }

        val pooled = acquireFromPool(source)
        if (pooled == null) {
            logDrop("pool_empty")
            return
        }

        try {
            copySnapshot(source, pooled)
            if (pooled is AceRawSnapshot) {
                pooled.fallback.seedFrom(pooled)
                applySnapshotAdapters(pooled)
            }
        } catch (e: Throwable) {
            releaseToPool(pooled)
            throw e
        }

        if (!channel.trySend(PollResult.Frame(pooled)).isSuccess) {
            releaseToPool(pooled)
            logDrop("queue_full")
        }
    }

    private fun applySnapshotAdapters(snapshot: AceRawSnapshot) {
        boundAceSnapshot = snapshot
        snapshotAdapterContext.snapshot = snapshot
        snapshotAdapterContext.gameState = currentState
        snapshotAdapterContext.loopStartNanos = snapshot.timestampNs
        snapshotAdapterContext.shouldApplyFallbackPatch = snapshot.needsFallbackPatch
        try {
            snapshotPipeline.apply(snapshotAdapterContext)
        } finally {
            boundAceSnapshot = null
        }
    }

    private fun logDrop(reason: String) {
        logger.atWarn(RATE_LIMITED) { message = "dropped poll result ($reason)" }
    }

    private fun drainPendingSnapshots(channel: Channel<PollResult>) {
        while (true) {
            val result = channel.tryReceive().getOrNull() ?: break
            if (result is PollResult.Frame) {
                release(result.snapshot)
            }
        }
    }

    private fun acquireFromPool(source: AcPollSnapshot): AcPollSnapshot? = when (source) {
        is AcLegacyRawSnapshot -> legacySnapshotPool.pollFirst()
        is AceRawSnapshot -> aceSnapshotPool.pollFirst()
    }

    private fun releaseToPool(snapshot: AcPollSnapshot) {
        when (snapshot) {
            is AcLegacyRawSnapshot -> legacySnapshotPool.offerLast(snapshot)
            is AceRawSnapshot -> aceSnapshotPool.offerLast(snapshot)
        }
    }

    private fun newLegacySnapshot(): AcLegacyRawSnapshot = AcLegacyRawSnapshot(
        physics = SPageFilePhysics(),
        graphics = SPageFileGraphics(),
        statics = SPageFileStatic(),
    )

    private fun newAceSnapshot(): AceRawSnapshot = AceRawSnapshot()

    private fun copySnapshot(source: AcPollSnapshot, target: AcPollSnapshot) {
        when {
            source is AcLegacyRawSnapshot && target is AcLegacyRawSnapshot -> copyLegacySnapshot(source, target)

            source is AceRawSnapshot && target is AceRawSnapshot -> copyAceSnapshot(source, target)

            else -> error(
                "Snapshot type mismatch source=${source::class.simpleName} target=${target::class.simpleName}",
            )
        }

        target.timestampNs = source.timestampNs
        target.frameId = source.frameId
        target.sessionRestartHint = source.sessionRestartHint
    }

    private fun copyLegacySnapshot(source: AcLegacyRawSnapshot, target: AcLegacyRawSnapshot) {
        copyPage(source.physics.pointer, target.physics.pointer, legacyPhysicsBuffer)
        copyPage(source.graphics.pointer, target.graphics.pointer, legacyGraphicsBuffer)
        copyPage(source.statics.pointer, target.statics.pointer, legacyStaticsBuffer)
        target.physics.read()
        target.graphics.read()
        target.statics.read()
    }

    private fun copyAceSnapshot(source: AceRawSnapshot, target: AceRawSnapshot) {
        copyPage(source.physics.pointer, target.physics.pointer, acePhysicsBuffer)
        copyPage(source.graphics.pointer, target.graphics.pointer, aceGraphicsBuffer)
        copyPage(source.statics.pointer, target.statics.pointer, aceStaticsBuffer)
        target.physics.read()
        target.graphics.read()
        target.statics.read()
        target.needsFallbackPatch = source.needsFallbackPatch
    }

    private fun copyPage(source: com.sun.jna.Pointer, target: com.sun.jna.Pointer, buffer: ByteArray) {
        source.read(0, buffer, 0, buffer.size)
        target.write(0, buffer, 0, buffer.size)
    }

    private fun requireBoundAceSnapshot(): AceRawSnapshot = requireNotNull(
        boundAceSnapshot,
    ) { "ACE snapshot is not bound" }

    private inner class SnapshotAdapterContext : AcPollSnapshotAdapterContext {

        override lateinit var snapshot: AceRawSnapshot
        override var gameState: GameConnectionState = GameConnectionState.DISCONNECTED
        override var loopStartNanos: Long = 0L
        override var shouldApplyFallbackPatch: Boolean = false

        override fun withFallbackMemory(block: (AcFallbackPatchMemory) -> Unit) {
            block(fallbackMemory)
        }
    }

    private fun ensurePollDispatcher(): ExecutorCoroutineDispatcher {
        val existing = pollDispatcher
        if (existing != null) return existing

        val created = Executors.newSingleThreadExecutor { r ->
            Thread(r, "ac-poll").apply { isDaemon = true }
        }.asCoroutineDispatcher()
        pollDispatcher = created
        return created
    }

    private fun pollLaunchContext(): CoroutineContext =
        if (useDedicatedPollThread) ensurePollDispatcher() else EmptyCoroutineContext

    private fun closePollDispatcher() {
        pollDispatcher?.close()
        pollDispatcher = null
    }

    private companion object {

        const val SNAPSHOT_POOL_SIZE = 8
        const val STATE_BUFFER_CAPACITY = 16
    }
}
