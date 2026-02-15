package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.structure.toKString
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.utils.NsRateLimiter
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.util.concurrent.locks.LockSupport
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(SessionScope::class)
class AcPollLoop(
    private val shm: AcSharedMemory,
    private val cfg: AcPollConfig,
) {

    private val reusableSnapshot = AcRawSnapshot(
        physics = shm.physics,
        graphics = shm.graphics,
        statics = shm.statics,
    )

    private val errorLogLimiter = NsRateLimiter(ERROR_LOG_INTERVAL_NS)
    private val noChangeLogLimiter = NsRateLimiter(NO_CHANGE_LOG_INTERVAL_NS)
    private var lastNeedsFallback: Boolean = false
    private var lastEmittedFrameNs: Long = 0L
    private var currentState: GameConnectionState = GameConnectionState.DISCONNECTED
    private var currentDataSource: DataSourceType = DataSourceType.NATIVE
    private var lastDetectedPhysicsPacket: Int = -1
    private var lastDetectedGraphicsPacket: Int = -1
    private var stalePacketCounter: Int = 0
    private var activePacketCounter: Int = 0
    private var graphicsStaleCounter: Int = 0
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
                val now = System.nanoTime()
                if (errorLogLimiter.shouldLog(now)) {
                    logger.error(e) { "[poll] loop error state=$currentState source=$currentDataSource" }
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
        disconnectedPollMs = DISCONNECTED_POLL_MIN_MS
        shm.close()
    }

    private suspend fun dispatch(onResult: (PollResult) -> Unit) {
        shm.readAll()
        val detection = detectGameState()
        handleStateTransition(detection, onResult)

        when (detection.state) {
            GameConnectionState.DISCONNECTED -> idleLoop(onResult)
            GameConnectionState.IN_MENU -> menuLoop(onResult)
            GameConnectionState.IN_SESSION -> sessionLoop(onResult)
        }
    }

    private suspend fun idleLoop(onResult: (PollResult) -> Unit) {
        lastPhysicsPacket = -1
        lastGraphicsPacket = -1

        while (currentCoroutineContext().isActive) {
            delay(disconnectedPollMs)
            disconnectedPollMs = (disconnectedPollMs * 3 / 2).coerceAtMost(DISCONNECTED_POLL_MAX_MS)

            shm.readAll()
            val detection = detectGameState()

            if (detection.state != GameConnectionState.DISCONNECTED) {
                handleStateTransition(detection, onResult)
                return
            }
        }
    }

    private suspend fun menuLoop(onResult: (PollResult) -> Unit) {
        while (currentCoroutineContext().isActive) {
            val now = System.nanoTime()

            frameId++
            reusableSnapshot.frameId = frameId
            reusableSnapshot.timestampNs = now
            onResult(PollResult.Frame(reusableSnapshot))

            delay(cfg.menuPollMs)

            shm.readAll()
            val detection = detectGameState()

            if (detection.state != GameConnectionState.IN_MENU) {
                handleStateTransition(detection, onResult)
                return
            }

            handleFallbackChange(detection)
        }
    }

    private suspend fun sessionLoop(onResult: (PollResult) -> Unit) {
        var nextTickNanos = System.nanoTime()

        while (currentCoroutineContext().isActive) {
            val now = System.nanoTime()

            shm.readAll()
            val physics = shm.physics
            val graphics = shm.graphics

            val detection = detectGameState()

            if (detection.state != GameConnectionState.IN_SESSION) {
                handleStateTransition(detection, onResult)
                return
            }

            handleFallbackChange(detection)

            val physicsPacket = physics.packetId
            val graphicsPacket = graphics.packetId

            val hasChanges = physicsPacket != lastPhysicsPacket ||
                graphicsPacket != lastGraphicsPacket

            if (hasChanges) {
                lastPhysicsPacket = physicsPacket
                lastGraphicsPacket = graphicsPacket

                frameId++
                reusableSnapshot.frameId = frameId
                reusableSnapshot.timestampNs = now
                onResult(PollResult.Frame(reusableSnapshot))

                lastEmittedFrameNs = now
            } else {
                if (noChangeLogLimiter.shouldLog(now)) {
                    val sinceMs = (now - lastEmittedFrameNs) / NS_PER_MS
                    val g = graphics
                    logger.debug {
                        "[poll] IN_SESSION but no packet changes for ${sinceMs}ms " +
                            "gfx(packet=${g.packetId} status=${g.status} session=${g.session} idx=${g.sessionIndex}) " +
                            "phy(packet=${physics.packetId} stale=$stalePacketCounter)"
                    }
                }
            }

            nextTickNanos += cfg.pollIntervalNanos

            val currentNanos = System.nanoTime()
            if (currentNanos - nextTickNanos > cfg.maxDriftNanos) {
                nextTickNanos = currentNanos
            }

            val remainingNs = nextTickNanos - currentNanos

            when {
                remainingNs > DELAY_THRESHOLD_NS -> delay(remainingNs / NS_PER_MS)
                remainingNs > 0L -> LockSupport.parkNanos(remainingNs)
                else -> yield()
            }
        }
    }

    private fun handleStateTransition(
        detection: Detection,
        onResult: (PollResult) -> Unit,
    ) {
        if (detection.state == currentState && detection.dataSource == currentDataSource) {
            handleFallbackChange(detection)
            return
        }

        val oldState = currentState
        val oldSource = currentDataSource

        currentState = detection.state
        currentDataSource = detection.dataSource

        logger.info {
            val g = shm.graphics
            val p = shm.physics
            val stateStr = "[poll] state: $oldState/$oldSource -> ${detection.state}/${detection.dataSource} "
            val fallbackStr = "needsFallback=${detection.needsFallback} "
            val gfxStr = "gfx(packet=${g.packetId}, status=${g.status}, session=${g.session}, " +
                "idx=${g.sessionIndex}, laps=${g.completedLaps}, left=${g.sessionTimeLeft}) "
            val phyStr = "phy(packet=${p.packetId}, rpm=${p.rpm}, speed=${p.speedKmh}, " +
                "stale=$stalePacketCounter, active=$activePacketCounter)"
            stateStr + fallbackStr + gfxStr + phyStr
        }

        if (oldState == GameConnectionState.DISCONNECTED && detection.state != GameConnectionState.DISCONNECTED) {
            disconnectedPollMs = DISCONNECTED_POLL_MIN_MS
        }

        handleFallbackChange(detection)
        onResult(PollResult.StateChanged(detection.state, detection.dataSource))
    }

    private fun handleFallbackChange(detection: Detection) {
        if (detection.needsFallback != lastNeedsFallback) {
            lastNeedsFallback = detection.needsFallback
            logger.info { "[poll] needsFallback changed -> ${detection.needsFallback} (source=$currentDataSource)" }
        }
    }

    private fun detectGameState(): Detection {
        if (!shm.isAnyAttached()) {
            resetCounters()
            return Detection(
                state = GameConnectionState.DISCONNECTED,
                dataSource = DataSourceType.NATIVE,
                needsFallback = false
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

        val isNative = hasNativeGraphics || hasNativeStatic

        val canUseGraphicsStatus = hasNativeGraphics && graphics.status in STATUS_OFF..STATUS_PAUSE
        val setupMenuVisible = canUseGraphicsStatus && graphics.isSetupMenuVisible != 0
        val statusOverride = if (canUseGraphicsStatus) {
            when (graphics.status) {
                STATUS_OFF, STATUS_REPLAY, STATUS_PAUSE -> GameConnectionState.IN_MENU
                else -> if (setupMenuVisible) GameConnectionState.IN_MENU else null
            }
        } else {
            null
        }
        val statusHint = if (canUseGraphicsStatus && !graphicsIsStale && graphics.status == STATUS_LIVE) {
            GameConnectionState.IN_SESSION
        } else {
            null
        }

        val needsStaticPatch = statics.track.toKString().isBlank() ||
            statics.carModel.toKString().isBlank() ||
            statics.numCars <= 0 ||
            statics.numberOfSessions <= 0 ||
            statics.sectorCount <= 0
        val needsFallback = !hasNativeGraphics || needsStaticPatch

        if (isNative) {
            val state = when {
                statusOverride != null -> statusOverride
                hasPhysicsPacket && physicsIsActive -> GameConnectionState.IN_SESSION
                hasPhysicsPacket && physicsIsInactive -> GameConnectionState.IN_MENU
                statusHint != null -> statusHint
                else -> if (currentState == GameConnectionState.IN_SESSION) currentState else GameConnectionState.IN_MENU
            }

            return Detection(
                state = state,
                dataSource = DataSourceType.NATIVE,
                needsFallback = needsFallback
            )
        }

        val fallbackState = when {
            physicsIsActive -> GameConnectionState.IN_SESSION
            physicsIsInactive ->
                if (hasPhysicsPacket) {
                    GameConnectionState.IN_MENU
                } else {
                    GameConnectionState.DISCONNECTED
                }

            else -> when {
                currentState == GameConnectionState.IN_SESSION -> currentState
                hasPhysicsPacket -> GameConnectionState.IN_MENU
                else -> GameConnectionState.DISCONNECTED
            }
        }

        return Detection(
            state = fallbackState,
            dataSource = DataSourceType.FALLBACK,
            needsFallback = hasPhysicsPacket
        )
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

        val DISCONNECTED_POLL_MIN_MS = 100.milliseconds.inWholeMilliseconds
        val DISCONNECTED_POLL_MAX_MS = 2.seconds.inWholeMilliseconds

        val DELAY_THRESHOLD_NS = 5.milliseconds.inWholeNanoseconds

        val NS_PER_MS = 1.milliseconds.inWholeNanoseconds

        val ERROR_LOG_INTERVAL_NS = 5.seconds.inWholeNanoseconds
        val NO_CHANGE_LOG_INTERVAL_NS = 2.seconds.inWholeNanoseconds
    }
}
