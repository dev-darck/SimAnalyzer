package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.structure.toKString
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
class AcPollLoop(private val shm: AcSharedMemory, private val cfg: AcPollConfig) {

    private val logger = logger()
    private val reusableSnapshot = AcRawSnapshot(
        physics = shm.physics,
        graphics = shm.graphics,
        statics = shm.statics,
    )
    private var lastNeedsFallback: Boolean = false
    private var lastEmittedFrameNs: Long = 0L

    private var currentState: GameConnectionState = GameConnectionState.DISCONNECTED
    private var currentDataSource: DataSourceType = DataSourceType.NATIVE

    private var lastDetectedPhysicsPacket: Int = -1
    private var lastDetectedGraphicsPacket: Int = -1
    private var stalePacketCounter: Int = 0
    private var activePacketCounter: Int = 0
    private var graphicsStaleCounter: Int = 0
    private var pendingTransitionState: GameConnectionState? = null
    private var pendingTransitionDataSource: DataSourceType? = null
    private var pendingTransitionHits: Int = 0
    private var menuExitDebounceActive: Boolean = false

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
            } catch (e: Throwable) {
                val wasState = currentState

                runCatching { shm.close() }

                currentState = GameConnectionState.DISCONNECTED
                currentDataSource = DataSourceType.NATIVE
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

        frameId += 1L
        reusableSnapshot.frameId = frameId
        reusableSnapshot.timestampNs = now
        reusableSnapshot.sessionRestartHint = AcSessionRestartHint.NONE

        onResult(PollResult.Frame(reusableSnapshot))
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
        val physics = shm.physics
        val graphics = shm.graphics

        val physicsPacket = physics.packetId
        val graphicsPacket = graphics.packetId
        val hasChanges = physicsPacket != lastPhysicsPacket || graphicsPacket != lastGraphicsPacket

        if (hasChanges) {
            lastPhysicsPacket = physicsPacket
            lastGraphicsPacket = graphicsPacket

            frameId += 1L
            reusableSnapshot.frameId = frameId
            reusableSnapshot.timestampNs = now
            reusableSnapshot.sessionRestartHint = AcSessionRestartHint.NONE
            onResult(PollResult.Frame(reusableSnapshot))

            lastEmittedFrameNs = now
            return
        }

        logSessionNoChanges(now, graphics, physics)
    }

    private fun logSessionNoChanges(now: Long, graphics: SPageFileGraphics, physics: SPageFilePhysics) {
        logger.atDebug(RATE_LIMITED) {
            message = "IN_SESSION but no packet changes for ${now}ms " +
                "gfx(packet=${graphics.packetId} status=${graphics.status} session=${graphics.session} " +
                "idx=${graphics.sessionIndex}) " +
                "phy(packet=${physics.packetId} stale=$stalePacketCounter)"
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

    private fun handleStateTransition(detection: Detection, onResult: (PollResult) -> Unit) {
        if (detection.state == currentState && detection.dataSource == currentDataSource) {
            handleFallbackChange(detection)
            return
        }

        val oldState = currentState
        val oldSource = currentDataSource

        currentState = detection.state
        currentDataSource = detection.dataSource
        updateMenuExitDebounce(oldState, detection.state)

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

    private fun stabilizeDetection(detection: Detection): Detection {
        val threshold = transitionConfirmationThreshold(detection)
        if (threshold <= 1) {
            clearPendingTransition()
            return detection
        }

        if (detection.state == currentState && detection.dataSource == currentDataSource) {
            clearPendingTransition()
            return detection
        }

        if (pendingTransitionState == detection.state && pendingTransitionDataSource == detection.dataSource) {
            pendingTransitionHits = (pendingTransitionHits + 1).coerceAtMost(threshold)
        } else {
            pendingTransitionState = detection.state
            pendingTransitionDataSource = detection.dataSource
            pendingTransitionHits = 1
        }

        if (pendingTransitionHits >= threshold) {
            clearPendingTransition()
            return detection
        }

        return detection.copy(
            state = currentState,
            dataSource = currentDataSource,
        )
    }

    private fun transitionConfirmationThreshold(detection: Detection): Int = when {
        menuExitDebounceActive &&
            currentState == GameConnectionState.IN_MENU &&
            detection.state == GameConnectionState.IN_SESSION -> MENU_EXIT_CONFIRMATION_SAMPLES

        else -> 1
    }

    private fun updateMenuExitDebounce(oldState: GameConnectionState, newState: GameConnectionState) {
        menuExitDebounceActive = when {
            oldState == GameConnectionState.IN_SESSION && newState == GameConnectionState.IN_MENU -> true
            newState == GameConnectionState.DISCONNECTED -> false
            oldState == GameConnectionState.IN_MENU && newState == GameConnectionState.IN_SESSION -> false
            else -> menuExitDebounceActive
        }
    }

    private fun clearPendingTransition() {
        pendingTransitionState = null
        pendingTransitionDataSource = null
        pendingTransitionHits = 0
    }

    private fun logStateTransition(oldState: GameConnectionState, oldSource: DataSourceType, detection: Detection) {
        val g = shm.graphics
        val p = shm.physics

        logger.atDebug(RATE_LIMITED) {
            val stateStr = "$oldState($oldSource) -> ${detection.state}(${detection.dataSource}) "
            val fallbackStr = "fallback=${detection.needsFallback} "
            val gfxStr = "gfx(packet=${g.packetId}, status=${g.status}, session=${g.session}, " +
                "idx=${g.sessionIndex}, completedLaps=${g.completedLaps}, stale=$graphicsStaleCounter) "
            val phyStr = "phy(packet=${p.packetId}, rpm=${p.rpm}, speed=${p.speedKmh}, " +
                "stale=$stalePacketCounter, active=$activePacketCounter)"

            message = stateStr + fallbackStr + gfxStr + phyStr
        }
    }

    private fun handleFallbackChange(detection: Detection) {
        if (detection.needsFallback == lastNeedsFallback) return

        lastNeedsFallback = detection.needsFallback
        logger.debug { "needsFallback changed -> ${detection.needsFallback} (source=$currentDataSource)" }
    }

    private fun detectGameState(): Detection {
        if (!shm.isAnyAttached()) {
            resetCounters()
            return Detection(
                state = GameConnectionState.DISCONNECTED,
                dataSource = DataSourceType.NATIVE,
                needsFallback = false,
            )
        }

        val physics = shm.physics
        val graphics = shm.graphics
        val statics = shm.statics

        val graphicsIsStale = updateGraphicsStaleness(graphics)
        val hasNativeGraphics = hasGraphicsSignal(graphics)
        val hasNativeStatic = hasNativeSignature(statics)

        val hasPhysicsPacket = updatePhysicsActivity(physics)
        val physicsIsActive = activePacketCounter >= ACTIVE_PACKET_THRESHOLD
        val physicsIsInactive = stalePacketCounter >= STALE_PACKET_THRESHOLD

        val statusOverride = resolveStatusOverride(graphics, hasNativeGraphics)
        val statusHint = resolveStatusHint(graphics, hasNativeGraphics, graphicsIsStale)

        val needsStaticPatch = statics.track.toKString().isBlank() ||
            statics.carModel.toKString().isBlank() ||
            statics.numCars <= 0 ||
            statics.numberOfSessions <= 0 ||
            statics.sectorCount <= 0

        val isNative = hasNativeGraphics || hasNativeStatic
        val needsFallback = !hasNativeGraphics || needsStaticPatch

        if (isNative) {
            return Detection(
                state = resolveNativeState(
                    statusOverride = statusOverride,
                    statusHint = statusHint,
                    hasPhysicsPacket = hasPhysicsPacket,
                    physicsIsActive = physicsIsActive,
                    physicsIsInactive = physicsIsInactive,
                ),
                dataSource = DataSourceType.NATIVE,
                needsFallback = needsFallback,
            )
        }

        return Detection(
            state = resolveFallbackState(
                hasPhysicsPacket = hasPhysicsPacket,
                physicsIsActive = physicsIsActive,
                physicsIsInactive = physicsIsInactive,
            ),
            dataSource = DataSourceType.FALLBACK,
            needsFallback = hasPhysicsPacket,
        )
    }

    private fun resolveStatusOverride(graphics: SPageFileGraphics, hasNativeGraphics: Boolean): GameConnectionState? {
        if (!hasNativeGraphics) return null
        if (graphics.status !in STATUS_OFF..STATUS_PAUSE) return null

        val setupMenuVisible = graphics.isSetupMenuVisible != 0
        return when (graphics.status) {
            STATUS_OFF, STATUS_REPLAY, STATUS_PAUSE -> GameConnectionState.IN_MENU
            else -> if (setupMenuVisible) GameConnectionState.IN_MENU else null
        }
    }

    private fun resolveStatusHint(
        graphics: SPageFileGraphics,
        hasNativeGraphics: Boolean,
        graphicsIsStale: Boolean,
    ): GameConnectionState? {
        if (!hasNativeGraphics) return null
        if (graphicsIsStale) return null
        if (graphics.status != STATUS_LIVE) return null
        return GameConnectionState.IN_SESSION
    }

    private fun resolveNativeState(
        statusOverride: GameConnectionState?,
        statusHint: GameConnectionState?,
        hasPhysicsPacket: Boolean,
        physicsIsActive: Boolean,
        physicsIsInactive: Boolean,
    ): GameConnectionState = when {
        statusOverride != null -> statusOverride
        hasPhysicsPacket && physicsIsActive -> GameConnectionState.IN_SESSION
        hasPhysicsPacket && physicsIsInactive -> GameConnectionState.IN_MENU
        statusHint != null -> statusHint
        currentState == GameConnectionState.IN_SESSION -> currentState
        else -> GameConnectionState.IN_MENU
    }

    private fun resolveFallbackState(
        hasPhysicsPacket: Boolean,
        physicsIsActive: Boolean,
        physicsIsInactive: Boolean,
    ): GameConnectionState = when {
        physicsIsActive -> GameConnectionState.IN_SESSION

        physicsIsInactive -> {
            if (hasPhysicsPacket) {
                GameConnectionState.IN_MENU
            } else {
                GameConnectionState.DISCONNECTED
            }
        }

        else -> when {
            currentState == GameConnectionState.IN_SESSION -> currentState
            hasPhysicsPacket -> GameConnectionState.IN_MENU
            else -> GameConnectionState.DISCONNECTED
        }
    }

    private fun hasNativeSignature(statics: SPageFileStatic): Boolean {
        if (statics.smVersion.toKString().isNotBlank()) return true
        if (statics.acVersion.toKString().isNotBlank()) return true
        if (statics.track.toKString().isNotBlank()) return true
        if (statics.carModel.toKString().isNotBlank()) return true
        return false
    }

    private fun hasGraphicsSignal(graphics: SPageFileGraphics): Boolean {
        if (graphics.packetId > 0) return true

        if (graphics.status != STATUS_OFF) return true
        if (graphics.iCurrentTime > 0) return true
        if (graphics.completedLaps > 0) return true
        if (graphics.sessionTimeLeft > 0f) return true
        if (graphics.distanceTraveled > 1f) return true
        if (graphics.activeCars > 0) return true
        if (graphics.position > 0) return true
        if (graphics.normalizedCarPosition > 0f) return true

        return false
    }

    private fun updateGraphicsStaleness(graphics: SPageFileGraphics): Boolean {
        val packetId = graphics.packetId
        val hasPacket = packetId > 0
        val packetChanged = hasPacket && packetId != lastDetectedGraphicsPacket

        if (packetChanged) {
            lastDetectedGraphicsPacket = packetId
            graphicsStaleCounter = 0
        } else {
            graphicsStaleCounter = (graphicsStaleCounter + 1).coerceAtMost(STALE_PACKET_THRESHOLD)
        }

        return graphicsStaleCounter >= STALE_PACKET_THRESHOLD
    }

    private fun updatePhysicsActivity(physics: SPageFilePhysics): Boolean {
        val packetId = physics.packetId
        val hasPacket = packetId > 0
        val packetChanged = hasPacket && packetId != lastDetectedPhysicsPacket

        if (packetChanged) {
            lastDetectedPhysicsPacket = packetId
            activePacketCounter = (activePacketCounter + 1).coerceAtMost(ACTIVE_PACKET_THRESHOLD)
            stalePacketCounter = 0
        } else {
            stalePacketCounter = (stalePacketCounter + 1).coerceAtMost(STALE_PACKET_THRESHOLD)
            if (stalePacketCounter >= STALE_PACKET_THRESHOLD) {
                activePacketCounter = 0
            }
        }

        return hasPacket
    }

    private fun resetCounters() {
        lastDetectedPhysicsPacket = -1
        lastDetectedGraphicsPacket = -1
        stalePacketCounter = 0
        activePacketCounter = 0
        graphicsStaleCounter = 0
    }

    private fun resetTransitionStability() {
        clearPendingTransition()
        menuExitDebounceActive = false
    }

    private data class Detection(
        val state: GameConnectionState,
        val dataSource: DataSourceType,
        val needsFallback: Boolean,
    )

    private companion object {

        const val STATUS_OFF = 0
        const val STATUS_REPLAY = 1
        const val STATUS_LIVE = 2
        const val STATUS_PAUSE = 3

        const val STALE_PACKET_THRESHOLD = 30
        const val ACTIVE_PACKET_THRESHOLD = 5
        const val MENU_EXIT_CONFIRMATION_SAMPLES = 10

        val DISCONNECTED_POLL_MIN_MS = 100.milliseconds.inWholeMilliseconds
        val DISCONNECTED_POLL_MAX_MS = 2.seconds.inWholeMilliseconds
    }
}
