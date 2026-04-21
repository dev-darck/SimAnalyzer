package com.project.analyzer.ac.telemetry.impl.internal.poll

import com.project.analyzer.ac.telemetry.impl.internal.poll.backend.AcPollBackend
import com.project.analyzer.ac.telemetry.impl.internal.poll.backend.ac.AcLegacyPollBackend
import com.project.analyzer.ac.telemetry.impl.internal.poll.backend.ace.AcePollBackend
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcLegacyRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcPollSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcSessionRestartHint
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout
import com.project.analyzer.ac.telemetry.impl.shm.ac.AcLegacySharedMemoryView
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.ace.AceSharedMemoryView
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.locks.LockSupport
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(SessionScope::class)
internal class AcPollLoop(private val shm: AcSharedMemory, private val cfg: AcPollConfig) {

    private val logger = logger()
    private val detector = AcPollDetector()
    private val transitionStability = AcPollTransitionStability()
    private val reusableLegacySnapshot = AcLegacyRawSnapshot(
        physics = SPageFilePhysics(),
        graphics = SPageFileGraphics(),
        statics = SPageFileStatic(),
    )
    private val reusableAceSnapshot = AceRawSnapshot()
    private var legacyBackend: AcLegacyPollBackend? = null
    private var aceBackend: AcePollBackend? = null
    private var lastEmittedFrameNs: Long = 0L

    private var currentState: GameConnectionState = GameConnectionState.DISCONNECTED
    private var currentDataSource: DataSourceType = DataSourceType.NATIVE
    private var currentNeedsFallback: Boolean = false

    private var disconnectedPollMs: Long = DISCONNECTED_POLL_MIN_MS
    private var frameId: Long = 0L
    private var lastPhysicsPacket: Int = -1
    private var lastGraphicsPacket: Int = -1

    suspend fun start(onResult: (PollResult) -> Unit) {
        while (currentCoroutineContext().isActive) {
            try {
                dispatch(onResult)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val wasState = currentState

                runCatching { shm.close() }

                currentState = GameConnectionState.DISCONNECTED
                currentDataSource = DataSourceType.NATIVE
                currentNeedsFallback = false
                resetCounters()
                resetTransitionStability()
                lastPhysicsPacket = -1
                lastGraphicsPacket = -1
                disconnectedPollMs = DISCONNECTED_POLL_MIN_MS

                logger.error(e) {
                    "poll loop error (was state=$wasState), shm closed, treating as disconnect"
                }

                runCatching {
                    onResult(
                        PollResult.StateChanged(
                            state = GameConnectionState.DISCONNECTED,
                            dataSource = DataSourceType.NATIVE,
                        ),
                    )
                }

                if (currentCoroutineContext().isActive) {
                    delay(cfg.reconnectDelayMs)
                }
            }
        }
    }

    fun stop() {
        currentState = GameConnectionState.DISCONNECTED
        currentDataSource = DataSourceType.NATIVE
        currentNeedsFallback = false

        resetCounters()
        resetTransitionStability()
        disconnectedPollMs = DISCONNECTED_POLL_MIN_MS
        shm.close()
    }

    private suspend fun dispatch(onResult: (PollResult) -> Unit) {
        shm.readAll()

        val detection = stabilizeDetection(detectGameState())
        handleStateTransition(detection, onResult)

        when (detection.state) {
            GameConnectionState.DISCONNECTED -> runDisconnectedLoop(onResult)
            GameConnectionState.IN_MENU -> runMenuLoop(onResult)
            GameConnectionState.IN_SESSION -> runSessionLoop(currentCoroutineContext(), onResult)
        }
    }

    private suspend fun runDisconnectedLoop(onResult: (PollResult) -> Unit) {
        lastPhysicsPacket = -1
        lastGraphicsPacket = -1

        while (currentCoroutineContext().isActive) {
            delay(disconnectedPollMs)
            disconnectedPollMs = (disconnectedPollMs * 3 / 2).coerceAtMost(DISCONNECTED_POLL_MAX_MS)

            shm.readAll()
            val detection = stabilizeDetection(detectGameState())
            if (detection.state != GameConnectionState.DISCONNECTED) {
                handleStateTransition(detection, onResult)
                return
            }
        }
    }

    private suspend fun runMenuLoop(onResult: (PollResult) -> Unit) {
        while (currentCoroutineContext().isActive) {
            emitMenuFrame(onResult)

            delay(cfg.menuPollMs)

            shm.readAll()
            val detection = stabilizeDetection(detectGameState())
            if (detection.state != GameConnectionState.IN_MENU) {
                handleStateTransition(detection, onResult)
                return
            }

            handleFallbackChange(detection)
        }
    }

    private fun emitMenuFrame(onResult: (PollResult) -> Unit) {
        val now = System.nanoTime()
        val snapshot = captureCurrentSnapshot()

        frameId += 1L
        snapshot.frameId = frameId
        snapshot.timestampNs = now
        snapshot.sessionRestartHint = AcSessionRestartHint.NONE

        onResult(PollResult.Frame(snapshot))
    }

    private suspend fun runSessionLoop(coroutineContext: CoroutineContext, onResult: (PollResult) -> Unit) {
        var nextTickNanos = System.nanoTime()

        while (coroutineContext.isActive) {
            val now = System.nanoTime()

            shm.readAll()
            val detection = stabilizeDetection(detectGameState())

            if (detection.state != GameConnectionState.IN_SESSION) {
                handleStateTransition(detection, onResult)
                return
            }

            handleFallbackChange(detection)
            emitSessionFrameIfNeeded(now, onResult)

            nextTickNanos = sleepUntilNextTick(nextTickNanos + cfg.pollIntervalNanos)
        }
    }

    private fun emitSessionFrameIfNeeded(now: Long, onResult: (PollResult) -> Unit) {
        val physicsPacket = currentPhysicsPacketId()
        val graphicsPacket = currentGraphicsPacketId()
        val hasChanges = physicsPacket != lastPhysicsPacket || graphicsPacket != lastGraphicsPacket

        if (hasChanges) {
            lastPhysicsPacket = physicsPacket
            lastGraphicsPacket = graphicsPacket
            val snapshot = captureCurrentSnapshot()

            frameId += 1L
            snapshot.frameId = frameId
            snapshot.timestampNs = now
            snapshot.sessionRestartHint = AcSessionRestartHint.NONE
            onResult(PollResult.Frame(snapshot))

            lastEmittedFrameNs = now
            return
        }

        logSessionNoChanges(now)
    }

    private fun logSessionNoChanges(now: Long) {
        val graphicsPacket = currentGraphicsPacketId()
        val graphicsStatus = currentGraphicsStatus()
        val graphicsSession = currentGraphicsSession()
        val graphicsSessionIndex = currentGraphicsSessionIndex()
        val graphicsCompletedLaps = activeBackend()?.graphicsCompletedLaps() ?: 0
        val physicsPacket = currentPhysicsPacketId()
        val physicsRpm = currentPhysicsRpm()
        val physicsSpeed = currentPhysicsSpeedKmh()
        logger.atDebug(RATE_LIMITED) {
            message = "IN_SESSION but no packet changes for ${now}ms " +
                "gfx(packet=$graphicsPacket status=$graphicsStatus session=$graphicsSession " +
                "idx=$graphicsSessionIndex laps=$graphicsCompletedLaps) " +
                "phy(packet=$physicsPacket rpm=$physicsRpm speed=$physicsSpeed stale=${detector.stalePacketCounter})"
        }
    }

    private suspend fun sleepUntilNextTick(scheduledTickNs: Long): Long {
        val nowNs = System.nanoTime()

        val nextTickNs = when {
            scheduledTickNs > nowNs -> scheduledTickNs

            // Late tick: skip missed slots instead of spinning in zero-sleep catch-up bursts.
            nowNs - scheduledTickNs > cfg.maxDriftNanos -> nowNs

            else -> {
                val missedIntervals = ((nowNs - scheduledTickNs) / cfg.pollIntervalNanos) + 1L
                scheduledTickNs + missedIntervals * cfg.pollIntervalNanos
            }
        }

        val remainingNs = nextTickNs - nowNs
        if (remainingNs > 0L) {
            if (remainingNs >= 1_000_000L) {
                delay(remainingNs.nanoseconds)
            } else {
                LockSupport.parkNanos(remainingNs)
            }
        }

        return nextTickNs
    }

    private fun handleStateTransition(detection: AcPollDetection, onResult: (PollResult) -> Unit) {
        if (detection.state == currentState && detection.dataSource == currentDataSource) {
            handleFallbackChange(detection)
            return
        }

        val oldState = currentState
        val oldSource = currentDataSource

        currentState = detection.state
        currentDataSource = detection.dataSource
        currentNeedsFallback = detection.needsFallback
        transitionStability.onStateChanged(oldState, detection.state)

        logStateTransition(oldState, oldSource, detection)

        if (oldState == GameConnectionState.DISCONNECTED && detection.state != GameConnectionState.DISCONNECTED) {
            disconnectedPollMs = DISCONNECTED_POLL_MIN_MS
        }

        onResult(
            PollResult.StateChanged(
                state = detection.state,
                dataSource = detection.dataSource,
            ),
        )
    }

    private fun stabilizeDetection(detection: AcPollDetection): AcPollDetection = transitionStability.stabilize(
        detection = detection,
        currentState = currentState,
        currentDataSource = currentDataSource,
    )

    private fun logStateTransition(
        oldState: GameConnectionState,
        oldSource: DataSourceType,
        detection: AcPollDetection,
    ) {
        logger.atDebug(RATE_LIMITED) {
            val stateStr = "$oldState($oldSource) -> ${detection.state}(${detection.dataSource}) "
            val fallbackStr = "fallback=${detection.needsFallback} "
            val backend = activeBackend()
            val gfxStr = "gfx(packet=${backend?.graphicsPacketId() ?: 0}, status=${backend?.graphicsStatus() ?: 0}, " +
                "session=${backend?.graphicsSession() ?: 0}, idx=${backend?.graphicsSessionIndex() ?: 0}, " +
                "completedLaps=${backend?.graphicsCompletedLaps() ?: 0}, stale=${detector.graphicsStaleCounter}) "
            val phyStr =
                "phy(packet=${backend?.physicsPacketId() ?: 0}, rpm=${backend?.physicsRpm() ?: 0}, speed=${backend?.physicsSpeedKmh() ?: 0f}, " +
                    "stale=${detector.stalePacketCounter}, active=${detector.activePacketCounter})"

            message = stateStr + fallbackStr + gfxStr + phyStr
        }
    }

    private fun handleFallbackChange(detection: AcPollDetection) {
        currentNeedsFallback = detection.needsFallback
        if (!transitionStability.consumeFallbackChange(detection.needsFallback)) return
        logger.debug { "needsFallback changed -> ${detection.needsFallback} (source=$currentDataSource)" }
    }

    private fun detectGameState(): AcPollDetection = detector.detect(currentState, activeBackend())

    private fun resetCounters() = detector.reset()

    private fun resetTransitionStability() = transitionStability.reset()

    private fun captureCurrentSnapshot(): AcPollSnapshot {
        val backend = requireNotNull(activeBackend()) { "No active poll backend for ${shm.layout}" }
        val snapshot = when (backend.layout) {
            AcSharedMemoryLayout.LEGACY -> reusableLegacySnapshot
            AcSharedMemoryLayout.ACEVO -> reusableAceSnapshot
            AcSharedMemoryLayout.UNKNOWN -> error("Cannot capture UNKNOWN layout snapshot")
        }
        backend.captureInto(snapshot)
        if (snapshot is AceRawSnapshot) {
            snapshot.needsFallbackPatch = currentNeedsFallback
        }
        return snapshot
    }

    private companion object {
        val DISCONNECTED_POLL_MIN_MS = 100.milliseconds.inWholeMilliseconds
        val DISCONNECTED_POLL_MAX_MS = 2.seconds.inWholeMilliseconds
    }

    private fun currentGraphicsPacketId(): Int = activeBackend()?.graphicsPacketId() ?: 0

    private fun currentGraphicsSession(): Int = activeBackend()?.graphicsSession() ?: 0

    private fun currentGraphicsSessionIndex(): Int = activeBackend()?.graphicsSessionIndex() ?: 0

    private fun currentGraphicsStatus(): Int = activeBackend()?.graphicsStatus() ?: 0

    private fun currentPhysicsPacketId(): Int = activeBackend()?.physicsPacketId() ?: 0

    private fun currentPhysicsRpm(): Int = activeBackend()?.physicsRpm() ?: 0

    private fun currentPhysicsSpeedKmh(): Float = activeBackend()?.physicsSpeedKmh() ?: 0f

    private fun activeBackend(): AcPollBackend? = when (val view = shm.view) {
        is AcLegacySharedMemoryView -> legacyBackend ?: AcLegacyPollBackend(view).also { legacyBackend = it }
        is AceSharedMemoryView -> aceBackend ?: AcePollBackend(view).also { aceBackend = it }
        else -> null
    }
}
