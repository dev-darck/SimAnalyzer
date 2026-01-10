package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.internal.AcLapState
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.toBoolean
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.contract.LapValidity
import com.project.analyzer.telemetry.ac.api.model.lap.LapFrame
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class LapMapper(
    private val cache: AcSessionCache,
    private val lapState: AcLapState
) {
    private var lastCompletedLaps: Int = -1
    private var bestValidLapTimeMs: Int? = null

    init {
        cache.addSessionChangeListener { reset() }
    }

    fun reset() {
        lastCompletedLaps = -1
        bestValidLapTimeMs = null
        lapState.reset()
    }

    fun map(graphics: SPageFileGraphics): LapFrame {
        val completedLaps = graphics.completedLaps
        val isValidLap = graphics.isValidLap.toBoolean()
        val lastLapTimeMs = graphics.iLastTime.takeIf { it > 0 }

        if (completedLaps != lastCompletedLaps) {
            if (lastCompletedLaps != -1) {
                if (isValidLap && lastLapTimeMs != null) {
                    bestValidLapTimeMs = bestValidLapTimeMs?.let { minOf(it, lastLapTimeMs) } ?: lastLapTimeMs
                }
                lapState.reset()
            }
            lastCompletedLaps = completedLaps
        }

        val sectors = lapState.onFrame(
            currentSectorIndex = graphics.currentSectorIndex,
            lastSectorTimeMs = graphics.lastSectorTime,
            isValidLap = isValidLap,
        )

        return LapFrame(
            currentLapIndex = completedLaps + 1,
            completedLaps = completedLaps,

            currentLapTimeMs = graphics.iCurrentTime,
            lastLapTimeMs = lastLapTimeMs,
            bestLapTimeMs = graphics.iBestTime.takeIf { it > 0 },
            bestValidLapTimeMs = bestValidLapTimeMs,

            sectorCount = cache.sectorCount,
            currentSectorIndex = graphics.currentSectorIndex,
            lastSectorTimeMs = graphics.lastSectorTime.takeIf { it > 0 },

            deltaLapTimeMs = graphics.iDeltaLapTime,
            isDeltaPositive = graphics.isDeltaPositive.toBoolean(),
            estimatedLapTimeMs = graphics.iEstimatedLapTime.takeIf { it > 0 },

            splitTimeMs = graphics.iSplit.takeIf { it > 0 },

            validity = LapValidity.fromBoolean(isValidLap),

            sectors = sectors,
        )
    }
}
