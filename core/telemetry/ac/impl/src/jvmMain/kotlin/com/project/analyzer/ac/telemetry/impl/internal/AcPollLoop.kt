package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.utils.NsRateLimiter
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException

@Inject
@SingleIn(SessionScope::class)
class AcPollLoop(
    private val shm: AcSharedMemory,
    private val cfg: AcPollConfig,
    private val fallback: AcEvoFallbackShmPatcher,
) {

    private val reusableSnapshot = AcRawSnapshot(
        physics = shm.physics,
        graphics = shm.graphics,
        statics = shm.statics,
    )

    private val errorLogLimiter = NsRateLimiter(5_000_000_000L)     // 5s
    private val noChangeLogLimiter = NsRateLimiter(2_000_000_000L)  // 2s
    private var lastNeedsFallback: Boolean = false
    private var lastEmittedFrameNs: Long = 0L
    private var currentState: GameConnectionState = GameConnectionState.DISCONNECTED
    private var currentDataSource: DataSourceType = DataSourceType.NATIVE
    private var lastDetectedPhysicsPacket: Int = -1
    private var stalePacketCounter: Int = 0
    private var activePacketCounter: Int = 0
    private var disconnectedPollMs: Long = DISCONNECTED_POLL_MIN_MS

    suspend fun start(onResult: suspend (PollResult) -> Unit) {
        var frameId = 0L

        while (currentCoroutineContext().isActive) {
            try {
                frameId = pollLoop(frameId, onResult)
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

    private suspend fun pollLoop(
        startFrameId: Long,
        onResult: suspend (PollResult) -> Unit
    ): Long {
        var frameId = startFrameId
        var lastPhysicsPacket = -1
        var lastGraphicsPacket = -1

        while (currentCoroutineContext().isActive) {
            val loopStartNanos = System.nanoTime()

            shm.readAll()
            val physics = shm.physics
            val graphics = shm.graphics

            val detection = detectGameState()

            if (detection.state != currentState || detection.dataSource != currentDataSource) {
                val oldState = currentState
                val oldSource = currentDataSource

                currentState = detection.state
                currentDataSource = detection.dataSource

                logger.info {
                    "[poll] state: $oldState/$oldSource -> ${detection.state}/${detection.dataSource} " +
                        "needsFallback=${detection.needsFallback} " +
                        "gfx(packet=${graphics.packetId}, status=${graphics.status}, session=${graphics.session}, idx=${graphics.sessionIndex}, laps=${graphics.completedLaps}, left=${graphics.sessionTimeLeft}) " +
                        "phy(packet=${physics.packetId}, rpm=${physics.rpm}, speed=${physics.speedKmh}, stale=$stalePacketCounter, active=$activePacketCounter)"
                }

                if (oldState == GameConnectionState.DISCONNECTED && detection.state != GameConnectionState.DISCONNECTED) {
                    disconnectedPollMs = DISCONNECTED_POLL_MIN_MS
                }

                onResult(PollResult.StateChanged(detection.state, detection.dataSource))
            }

            if (detection.needsFallback != lastNeedsFallback) {
                lastNeedsFallback = detection.needsFallback
                logger.info { "[poll] needsFallback changed -> ${detection.needsFallback} (source=$currentDataSource)" }
            }

            when (detection.state) {
                GameConnectionState.DISCONNECTED -> {
                    fallback.clear()
                    lastPhysicsPacket = -1
                    lastGraphicsPacket = -1

                    delay(disconnectedPollMs)
                    disconnectedPollMs = (disconnectedPollMs * 3 / 2).coerceAtMost(DISCONNECTED_POLL_MAX_MS)
                    continue
                }

                GameConnectionState.IN_MENU -> {
                    if (detection.dataSource == DataSourceType.FALLBACK) {
                        fallback.patchIfNeeded(shm, loopStartNanos, detection.state)
                    }

                    frameId++
                    reusableSnapshot.frameId = frameId
                    reusableSnapshot.timestampNs = loopStartNanos
                    onResult(PollResult.Frame(reusableSnapshot))

                    delay(cfg.menuPollMs)
                    continue
                }

                GameConnectionState.IN_SESSION -> {
                    if (detection.dataSource == DataSourceType.FALLBACK) {
                        fallback.patchIfNeeded(shm, loopStartNanos, detection.state)
                    }

                    val physicsPacket = physics.packetId
                    val graphicsPacket = graphics.packetId

                    val hasChanges = physicsPacket != lastPhysicsPacket ||
                        graphicsPacket != lastGraphicsPacket

                    if (hasChanges) {
                        lastPhysicsPacket = physicsPacket
                        lastGraphicsPacket = graphicsPacket

                        frameId++
                        reusableSnapshot.frameId = frameId
                        reusableSnapshot.timestampNs = loopStartNanos
                        onResult(PollResult.Frame(reusableSnapshot))

                        lastEmittedFrameNs = loopStartNanos
                    } else {
                        if (noChangeLogLimiter.shouldLog(loopStartNanos)) {
                            val sinceMs = (loopStartNanos - lastEmittedFrameNs) / 1_000_000
                            logger.debug {
                                "[poll] IN_SESSION but no packet changes for ${sinceMs}ms " +
                                    "gfx(packet=$graphicsPacket status=${graphics.status} session=${graphics.session} idx=${graphics.sessionIndex}) " +
                                    "phy(packet=$physicsPacket stale=$stalePacketCounter)"
                            }
                        }
                    }
                }
            }

            val elapsedNanos = System.nanoTime() - loopStartNanos
            val sleepNanos = cfg.pollIntervalNanos - elapsedNanos

            when {
                sleepNanos > 1_000_000 -> delay(sleepNanos / 1_000_000)
                else -> yield()
            }
        }

        return frameId
    }

    private fun detectGameState(): StateDetection {
        val isAttached = shm.isAnyAttached()
        if (!isAttached) {
            resetCounters()
            return StateDetection(
                state = GameConnectionState.DISCONNECTED,
                dataSource = DataSourceType.NATIVE,
                needsFallback = false
            )
        }

        val physics = shm.physics
        val graphics = shm.graphics

        val hasNativeGraphics = graphics.packetId > 0 &&
            (graphics.status > 0 || graphics.completedLaps > 0 || graphics.iCurrentTime > 0)

        val hasActivePhysics = isPhysicsActive(physics)

        return when {
            hasNativeGraphics -> {
                resetCounters()
                val state = when (graphics.status) {
                    STATUS_OFF -> GameConnectionState.IN_MENU
                    else -> GameConnectionState.IN_SESSION
                }
                StateDetection(
                    state = state,
                    dataSource = DataSourceType.NATIVE,
                    needsFallback = false
                )
            }

            hasActivePhysics -> {
                val currentPacket = physics.packetId

                if (currentPacket == lastDetectedPhysicsPacket) {
                    stalePacketCounter++
                    activePacketCounter = 0
                } else {
                    stalePacketCounter = 0
                    activePacketCounter++
                }
                lastDetectedPhysicsPacket = currentPacket

                val effectiveState = when (currentState) {
                    GameConnectionState.IN_SESSION -> {
                        if (stalePacketCounter > STALE_PACKET_THRESHOLD) {
                            GameConnectionState.IN_MENU
                        } else {
                            GameConnectionState.IN_SESSION
                        }
                    }

                    GameConnectionState.IN_MENU -> {
                        if (activePacketCounter >= ACTIVE_PACKET_THRESHOLD) {
                            GameConnectionState.IN_SESSION
                        } else {
                            GameConnectionState.IN_MENU
                        }
                    }

                    else -> {
                        if (activePacketCounter >= ACTIVE_PACKET_THRESHOLD) {
                            GameConnectionState.IN_SESSION
                        } else {
                            GameConnectionState.IN_MENU
                        }
                    }
                }

                StateDetection(
                    state = effectiveState,
                    dataSource = DataSourceType.FALLBACK,
                    needsFallback = effectiveState == GameConnectionState.IN_SESSION
                )
            }

            else -> {
                resetCounters()
                val physicsAttached = physics.packetId > 0

                StateDetection(
                    state = if (physicsAttached) GameConnectionState.IN_MENU else GameConnectionState.DISCONNECTED,
                    dataSource = DataSourceType.FALLBACK,
                    needsFallback = physicsAttached
                )
            }
        }
    }

    /**
     * Checks if physics data indicates an active session.
     * Uses multiple signals to be robust across AC/ACC/Evo.
     */
    private fun isPhysicsActive(physics: SPageFilePhysics): Boolean {
        if (physics.rpm > 0) return true

        if (physics.speedKmh > 0.1f) return true

        val contacts = physics.tyreContactPoint
        if (contacts.size >= 3) {
            val hasValidContact = contacts[0] != 0f || contacts[1] != 0f || contacts[2] != 0f
            if (hasValidContact) return true
        }

        if (physics.fuel > 0f) return true

        if (physics.gear != 0) return true

        return false
    }

    fun stop() {
        currentState = GameConnectionState.DISCONNECTED
        currentDataSource = DataSourceType.NATIVE
        resetCounters()
        disconnectedPollMs = DISCONNECTED_POLL_MIN_MS
        fallback.clear()
    }

    private fun resetCounters() {
        lastDetectedPhysicsPacket = -1
        stalePacketCounter = 0
        activePacketCounter = 0
    }

    private data class StateDetection(
        val state: GameConnectionState,
        val dataSource: DataSourceType,
        val needsFallback: Boolean
    )

    private companion object {

        const val STATUS_OFF = 0

        const val STALE_PACKET_THRESHOLD = 30
        const val ACTIVE_PACKET_THRESHOLD = 5

        const val DISCONNECTED_POLL_MIN_MS = 100L
        const val DISCONNECTED_POLL_MAX_MS = 2000L
    }
}
