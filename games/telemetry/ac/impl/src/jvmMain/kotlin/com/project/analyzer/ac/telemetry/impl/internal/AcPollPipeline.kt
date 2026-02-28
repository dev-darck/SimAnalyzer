package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.internal.pipeline.AcFallbackPollSnapshotAdapter
import com.project.analyzer.ac.telemetry.impl.internal.pipeline.AcPollSnapshotAdapter
import com.project.analyzer.ac.telemetry.impl.internal.pipeline.AcPollSnapshotAdapterContext
import com.project.analyzer.ac.telemetry.impl.internal.pipeline.AcPollSnapshotPipeline
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
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

internal class AcPollPipeline @Inject constructor(
    private val pollLoop: AcPollLoop,
    snapshotAdapters: Set<AcPollSnapshotAdapter>,
) {

    private val logger = logger()
    private val physicsSize = SPageFilePhysics().size()
    private val graphicsSize = SPageFileGraphics().size()
    private val staticsSize = SPageFileStatic().size()

    private val physicsBuffer = ByteArray(physicsSize)
    private val graphicsBuffer = ByteArray(graphicsSize)
    private val staticsBuffer = ByteArray(staticsSize)

    private val snapshotPool = ConcurrentLinkedDeque<AcRawSnapshot>()

    private var boundSnapshot: AcRawSnapshot? = null
    private val boundSnapshotMemory = object : AcSharedMemory {
        override val physics: SPageFilePhysics
            get() = requireBoundSnapshot().physics

        override val graphics: SPageFileGraphics
            get() = requireBoundSnapshot().graphics

        override val statics: SPageFileStatic
            get() = requireBoundSnapshot().statics

        override fun isAnyAttached(): Boolean = boundSnapshot != null

        override fun readAll() = Unit

        override fun close() = Unit
    }

    private var pollDispatcher: ExecutorCoroutineDispatcher? = null
    private var pollJob: Job? = null
    private var channel: Channel<PollResult>? = null
    private var currentState: GameConnectionState = GameConnectionState.DISCONNECTED
    private val snapshotPipeline = AcPollSnapshotPipeline(adapters = snapshotAdapters.toList())
    private val snapshotAdapterContext = SnapshotAdapterContext()

    init {
        repeat(SNAPSHOT_POOL_SIZE) {
            snapshotPool.addLast(newSnapshot())
        }
    }

    fun start(scope: CoroutineScope): Channel<PollResult> {
        channel?.let { return it }

        val created = Channel<PollResult>(capacity = SNAPSHOT_POOL_SIZE + STATE_BUFFER_CAPACITY)
        channel = created

        pollJob = scope.launch(ensurePollDispatcher()) {
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

    fun release(snapshot: AcRawSnapshot) {
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

    private fun handleFrame(source: AcRawSnapshot, channel: Channel<PollResult>) {
        val pooled = acquireFromPool()
        if (pooled == null) {
            logDrop("pool_empty")
            return
        }

        try {
            copySnapshot(source, pooled)
            readSnapshot(pooled)
        } catch (e: Throwable) {
            releaseToPool(pooled)
            throw e
        }

        try {
            applySnapshotAdapters(pooled)
        } catch (error: Exception) {
            releaseToPool(pooled)
            throw error
        }

        if (!channel.trySend(PollResult.Frame(pooled)).isSuccess) {
            releaseToPool(pooled)
            logDrop("queue_full")
        }
    }

    private fun applySnapshotAdapters(snapshot: AcRawSnapshot) {
        boundSnapshot = snapshot
        snapshotAdapterContext.snapshot = snapshot
        snapshotAdapterContext.gameState = currentState
        snapshotAdapterContext.loopStartNanos = snapshot.timestampNs
        try {
            snapshotPipeline.apply(snapshotAdapterContext)
        } finally {
            boundSnapshot = null
        }
    }

    private fun logDrop(reason: String) {
        logger.atWarn(RATE_LIMITED) { message = "dropped poll result ($reason)" }
    }

    private fun drainPendingSnapshots(channel: Channel<PollResult>) {
        while (true) {
            val result = channel.tryReceive().getOrNull() ?: break
            if (result is PollResult.Frame) {
                releaseToPool(result.snapshot)
            }
        }
    }

    private fun acquireFromPool(): AcRawSnapshot? = snapshotPool.pollFirst()

    private fun releaseToPool(snapshot: AcRawSnapshot) {
        snapshotPool.offerLast(snapshot)
    }

    private fun newSnapshot(): AcRawSnapshot = AcRawSnapshot(
        physics = SPageFilePhysics(),
        graphics = SPageFileGraphics(),
        statics = SPageFileStatic(),
    )

    private fun copySnapshot(source: AcRawSnapshot, target: AcRawSnapshot) {
        copyPhysics(source.physics, target.physics)
        copyGraphics(source.graphics, target.graphics)
        copyStatics(source.statics, target.statics)

        target.timestampNs = source.timestampNs
        target.frameId = source.frameId
        target.sessionRestartHint = source.sessionRestartHint
    }

    private fun readSnapshot(snapshot: AcRawSnapshot) {
        snapshot.physics.read()
        snapshot.graphics.read()
        snapshot.statics.read()
    }

    private fun copyPhysics(source: SPageFilePhysics, target: SPageFilePhysics) {
        source.pointer.read(0, physicsBuffer, 0, physicsSize)
        target.pointer.write(0, physicsBuffer, 0, physicsSize)
    }

    private fun copyGraphics(source: SPageFileGraphics, target: SPageFileGraphics) {
        source.pointer.read(0, graphicsBuffer, 0, graphicsSize)
        target.pointer.write(0, graphicsBuffer, 0, graphicsSize)
    }

    private fun copyStatics(source: SPageFileStatic, target: SPageFileStatic) {
        source.pointer.read(0, staticsBuffer, 0, staticsSize)
        target.pointer.write(0, staticsBuffer, 0, staticsSize)
    }

    private fun requireBoundSnapshot(): AcRawSnapshot = requireNotNull(boundSnapshot) { "Snapshot is not bound" }

    private inner class SnapshotAdapterContext : AcPollSnapshotAdapterContext {
        override lateinit var snapshot: AcRawSnapshot
        override var gameState: GameConnectionState = GameConnectionState.DISCONNECTED
        override var loopStartNanos: Long = 0L

        override fun withSharedMemory(block: (AcSharedMemory) -> Unit) {
            block(boundSnapshotMemory)
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

    private fun closePollDispatcher() {
        pollDispatcher?.close()
        pollDispatcher = null
    }

    private companion object {

        const val SNAPSHOT_POOL_SIZE: Int = 128
        const val STATE_BUFFER_CAPACITY: Int = 8
    }

    internal constructor(pollLoop: AcPollLoop, fallback: AcEvoFallbackShmPatcher) : this(
        pollLoop = pollLoop,
        snapshotAdapters = setOf(AcFallbackPollSnapshotAdapter(fallback)),
    )
}
