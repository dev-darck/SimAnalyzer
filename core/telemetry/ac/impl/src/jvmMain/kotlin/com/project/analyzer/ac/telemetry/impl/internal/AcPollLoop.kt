package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.api.di.SessionScope
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

    private var currentState: GameConnectionState = GameConnectionState.DISCONNECTED
    private var currentDataSource: DataSourceType = DataSourceType.NATIVE

    /**
     * Starts the poll loop. Emits PollResult for state changes and frames.
     * Runs until coroutine is cancelled.
     */
    suspend fun start(onResult: (PollResult) -> Unit) {
        var frameId = 0L

        while (currentCoroutineContext().isActive) {
            try {
                frameId = pollLoop(frameId, onResult)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("AC Shared Memory error: ${e.message}")
                if (currentCoroutineContext().isActive) {
                    delay(cfg.reconnectDelayMs)
                }
            }
        }
    }

    private suspend fun pollLoop(
        startFrameId: Long,
        onResult: (PollResult) -> Unit
    ): Long {
        var frameId = startFrameId
        var lastPhysicsPacket = -1
        var lastGraphicsPacket = -1

        while (currentCoroutineContext().isActive) {
            val loopStartNanos = System.nanoTime()

            shm.readAll()

            val detection = detectGameState()

            // Emit state change if needed
            if (detection.state != currentState || detection.dataSource != currentDataSource) {
                currentState = detection.state
                currentDataSource = detection.dataSource
                onResult(PollResult.StateChanged(detection.state, detection.dataSource))
            }

            // Handle based on current state
            when (detection.state) {
                GameConnectionState.DISCONNECTED -> {
                    // Game not running - slow poll
                    delay(cfg.gameNotRunningPollMs)
                    fallback.clear()
                    lastPhysicsPacket = -1
                    lastGraphicsPacket = -1
                    continue
                }

                GameConnectionState.IN_MENU -> {
                    // Game running but in menu - medium poll
                    // Apply fallback if needed (for AC Evo in menu state)
                    if (detection.needsFallback) {
                        fallback.patchIfNeeded(shm, loopStartNanos)
                    } else {
                        fallback.clear()
                    }

                    // Emit frame so UI knows current state
                    frameId++
                    reusableSnapshot.frameId = frameId
                    reusableSnapshot.timestampNs = loopStartNanos
                    onResult(PollResult.Frame(reusableSnapshot))

                    delay(cfg.menuPollMs)
                    continue
                }

                GameConnectionState.IN_SESSION -> {
                    // Active session - apply fallback if needed, then fast poll
                    if (detection.needsFallback) {
                        fallback.patchIfNeeded(shm, loopStartNanos)
                    }

                    val physicsPacket = shm.physics.packetId
                    val graphicsPacket = shm.graphics.packetId

                    val hasChanges = physicsPacket != lastPhysicsPacket ||
                        graphicsPacket != lastGraphicsPacket

                    if (hasChanges) {
                        lastPhysicsPacket = physicsPacket
                        lastGraphicsPacket = graphicsPacket
                        frameId++

                        reusableSnapshot.frameId = frameId
                        reusableSnapshot.timestampNs = loopStartNanos
                        onResult(PollResult.Frame(reusableSnapshot))
                    }
                }
            }

            val elapsedNanos = System.nanoTime() - loopStartNanos
            val sleepNanos = cfg.pollIntervalNanos - elapsedNanos

            when {
                sleepNanos > 1_000_000 -> {
                    delay(sleepNanos / 1_000_000)
                }
                sleepNanos > 100_000 -> {
                    yield()
                }
            }
        }

        return frameId
    }

    /**
     * Detects game state with fallback awareness.
     *
     * Logic:
     * 1. If SHM not attached → DISCONNECTED
     * 2. If physics has valid data (rpm > 0, or speed > 0, or tyres have contact) → game is running
     * 3. If graphics.status > 0 (native AC/ACC) → use native state detection
     * 4. If graphics empty but physics active → AC Evo mode, use fallback
     */
    private fun detectGameState(): StateDetection {
        val isAttached = shm.isAnyAttached()
        if (!isAttached) {
            return StateDetection(
                state = GameConnectionState.DISCONNECTED,
                dataSource = DataSourceType.NATIVE,
                needsFallback = false
            )
        }

        val physics = shm.physics
        val graphics = shm.graphics

        // Check if we have native graphics data
        val hasNativeGraphics = graphics.packetId > 0 &&
            (graphics.status > 0 || graphics.completedLaps > 0 || graphics.iCurrentTime > 0)

        // Check if physics indicates game is active
        val hasActivePhysics = isPhysicsActive(physics)

        return when {
            // Native AC/ACC mode - graphics has valid data
            hasNativeGraphics -> {
                val state = when (graphics.status) {
                    STATUS_OFF -> GameConnectionState.IN_MENU
                    else -> GameConnectionState.IN_SESSION // LIVE, REPLAY, PAUSE
                }
                StateDetection(
                    state = state,
                    dataSource = DataSourceType.NATIVE,
                    needsFallback = false
                )
            }

            // AC Evo mode - physics active but graphics empty
            hasActivePhysics -> {
                StateDetection(
                    state = GameConnectionState.IN_SESSION,
                    dataSource = DataSourceType.FALLBACK,
                    needsFallback = true
                )
            }

            // SHM attached but no activity - likely in menu
            else -> {
                // Check if physics has any sign of life (even if car is stationary)
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
        // Engine running or has RPM
        if (physics.rpm > 0) return true

        // Car is moving
        if (physics.speedKmh > 0.1f) return true

        // Tyres have contact with ground (valid contact points)
        val contacts = physics.tyreContactPoint
        if (contacts.size >= 3) {
            val hasValidContact = contacts[0] != 0f || contacts[1] != 0f || contacts[2] != 0f
            if (hasValidContact) return true
        }

        // Fuel present (car is loaded)
        if (physics.fuel > 0f) return true

        // Gear is set (not neutral and not invalid)
        if (physics.gear != 0) return true

        return false
    }

    fun stop() {
        currentState = GameConnectionState.DISCONNECTED
        currentDataSource = DataSourceType.NATIVE
        fallback.clear()
        reusableSnapshot.physics.clear()
        reusableSnapshot.statics.clear()
        reusableSnapshot.graphics.clear()
    }

    private data class StateDetection(
        val state: GameConnectionState,
        val dataSource: DataSourceType,
        val needsFallback: Boolean
    )

    companion object {

        private const val STATUS_OFF = 0
    }
}
