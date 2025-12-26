package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException

@Inject
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

    suspend fun run(onSnapshot: (AcRawSnapshot) -> Unit) {
        var frameId = 0L

        while (currentCoroutineContext().isActive) {
            try {
                frameId = pollLoop(frameId, onSnapshot)
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
        onSnapshot: (AcRawSnapshot) -> Unit
    ): Long {
        var frameId = startFrameId
        var lastPhysicsPacket = -1
        var lastGraphicsPacket = -1

        while (currentCoroutineContext().isActive) {
            val loopStartNanos = System.nanoTime()

            shm.readAll()

            val anyAttached = shm.isAnyAttached()
            if (!anyAttached) {
                delay(cfg.gameNotRunningPollMs)
                fallback.clear()
                continue
            }

            fallback.patchIfNeeded(shm, loopStartNanos)

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
                onSnapshot(reusableSnapshot)
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

    fun close() {
        fallback.clear()
    }
}
