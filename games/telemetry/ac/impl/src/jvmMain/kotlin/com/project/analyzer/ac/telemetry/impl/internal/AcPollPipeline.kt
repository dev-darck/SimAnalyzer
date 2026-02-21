package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.utils.NsRateLimiter
import com.project.analyzer.utils.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

internal class AcPollPipeline(private val pollLoop: AcPollLoop, private val fallback: AcEvoFallbackShmPatcher) {

    private val dropLogLimiter = NsRateLimiter(DROP_LOG_INTERVAL_NS)

    private val physicsSize = SPageFilePhysics().size()
    private val graphicsSize = SPageFileGraphics().size()
    private val staticsSize = SPageFileStatic().size()

    private val physicsBuffer = ByteArray(physicsSize)
    private val graphicsBuffer = ByteArray(graphicsSize)
    private val staticsBuffer = ByteArray(staticsSize)

    private val poolLock = Any()
    private val snapshotPool = ArrayDeque<AcRawSnapshot>(SNAPSHOT_POOL_SIZE)

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
        fallback.clear()
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
        if (result.state == GameConnectionState.DISCONNECTED) {
            fallback.clear()
        }
        if (!channel.trySend(result).isSuccess) {
            logDrop("state_change", System.nanoTime())
        }
    }

    private fun handleFrame(source: AcRawSnapshot, channel: Channel<PollResult>) {
        val pooled = acquireFromPool()
        if (pooled == null) {
            logDrop("pool_empty", source.timestampNs)
            return
        }

        copySnapshot(source, pooled)
        readSnapshot(pooled)

        try {
            patchWithFallback(pooled)
        } catch (error: Exception) {
            releaseToPool(pooled)
            throw error
        }

        if (!channel.trySend(PollResult.Frame(pooled)).isSuccess) {
            releaseToPool(pooled)
            logDrop("queue_full", pooled.timestampNs)
        }
    }

    private fun patchWithFallback(snapshot: AcRawSnapshot) {
        boundSnapshot = snapshot
        try {
            fallback.patchIfNeeded(
                shm = boundSnapshotMemory,
                loopStartNanos = snapshot.timestampNs.takeIf { it > 0L } ?: System.nanoTime(),
                gameState = currentState,
            )
        } finally {
            boundSnapshot = null
        }
    }

    private fun logDrop(reason: String, nowNs: Long) {
        val now = if (nowNs > 0L) nowNs else System.nanoTime()
        if (dropLogLimiter.shouldLog(now)) {
            logger.warn { "[lifecycle] dropped poll result ($reason)" }
        }
    }

    private fun drainPendingSnapshots(channel: Channel<PollResult>) {
        while (true) {
            val result = channel.tryReceive().getOrNull() ?: break
            if (result is PollResult.Frame) {
                releaseToPool(result.snapshot)
            }
        }
    }

    private fun acquireFromPool(): AcRawSnapshot? = synchronized(poolLock) {
        if (snapshotPool.isEmpty()) null else snapshotPool.removeFirst()
    }

    private fun releaseToPool(snapshot: AcRawSnapshot) {
        synchronized(poolLock) {
            snapshotPool.addLast(snapshot)
        }
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
        val DROP_LOG_INTERVAL_NS: Long = 2.seconds.inWholeNanoseconds
    }
}
