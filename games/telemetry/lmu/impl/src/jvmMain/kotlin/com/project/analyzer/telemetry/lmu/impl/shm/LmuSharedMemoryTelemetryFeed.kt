package com.project.analyzer.telemetry.lmu.impl.shm

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.lmu.api.model.LmuTelemetrySnapshot
import com.project.analyzer.telemetry.lmu.impl.LmuTelemetryFeed
import com.project.analyzer.telemetry.lmu.impl.mapper.LmuShmMapper
import com.project.analyzer.utils.shm.toBoolean
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(SessionScope::class)
internal class LmuSharedMemoryTelemetryFeed(
    private val shm: LmuSharedMemory,
    private val shmMapper: LmuShmMapper,
) : LmuTelemetryFeed {

    private var frameCounter: Long = 0L

    private var cachedPlayerIndex: Int? = null
    private var cachedPlayerIndexNs: Long = 0L

    override suspend fun collectFrames(onSnapshot: suspend (LmuTelemetrySnapshot) -> Unit) {
        while (currentCoroutineContext().isActive) {
            if (!shm.readAll()) {
                delay(RETRY_DELAY)
                continue
            }

            val snapshot = readFrame()
            if (snapshot != null) {
                onSnapshot(snapshot)
            }

            delay(FRAME_DELAY)
        }
    }

    override fun close() {
        shm.close()
    }

    private fun readFrame(): LmuTelemetrySnapshot? {
        val numVehicles = shm.numVehicles
        if (numVehicles <= 0) return null

        val playerIndex = resolvePlayerIndex(numVehicles).coerceIn(0, numVehicles - 1)

        val telemetry = shm.getVehicleTelemetry(playerIndex) ?: return null
        val scoring = shm.getVehicleScoring(playerIndex)
        val scoringInfo = shm.getScoringInfo()

        val versionBefore = shm.telemetryVersion
        val scoringVersionBefore = shm.scoringVersion.takeIf { it > 0 }

        frameCounter += 1L
        val snapshot = shmMapper.map(
            frameId = frameCounter,
            telemetryVersion = versionBefore,
            scoringVersion = scoringVersionBefore,
            numVehicles = numVehicles,
            playerIndex = playerIndex,
            telemetry = telemetry,
            scoring = scoring,
            scoringInfo = scoringInfo
        )

        var valid = shm.readAll() && shm.telemetryVersion == versionBefore
        if (scoringVersionBefore != null) {
            valid = valid && shm.scoringVersion == scoringVersionBefore
        }

        return if (valid) snapshot else null
    }

    private fun resolvePlayerIndex(numVehicles: Int): Int {
        val cached = cachedPlayerIndex
        val now = System.nanoTime()
        if (cached != null && now - cachedPlayerIndexNs < PLAYER_INDEX_TTL_NS) {
            return cached
        }

        var found: Int? = null
        for (index in 0 until numVehicles) {
            val scoring = shm.getVehicleScoring(index)
            if (scoring?.isPlayer?.toInt()?.toBoolean() == true) {
                found = index
                break
            }
        }

        if (found != null) {
            cachedPlayerIndex = found
            cachedPlayerIndexNs = now
            return found
        }

        return cached ?: 0
    }

    private companion object {

        val FRAME_DELAY = 20.milliseconds
        val RETRY_DELAY = 750.milliseconds
        val PLAYER_INDEX_TTL_NS: Long = 1.seconds.inWholeNanoseconds
    }
}
