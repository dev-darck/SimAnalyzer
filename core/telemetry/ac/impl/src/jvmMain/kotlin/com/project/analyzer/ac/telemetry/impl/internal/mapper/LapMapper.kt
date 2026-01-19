package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.internal.AcLapState
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.toBoolean
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.contract.LapValidity
import com.project.analyzer.telemetry.ac.api.model.lap.LapFrame
import com.project.analyzer.telemetry.ac.api.model.lap.SectorFrame
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
    private var lastLapSectors: List<SectorFrame> = emptyList()

    init {
        cache.addSessionChangeListener { reset() }
    }

    fun reset() {
        lastCompletedLaps = -1
        bestValidLapTimeMs = null
        lastLapSectors = emptyList()
        lapState.fullReset()
    }

    fun map(graphics: SPageFileGraphics): LapFrame {
        val completedLaps = graphics.completedLaps
        val isValidLap = graphics.isValidLap.toBoolean()
        val lastLapTimeMs = graphics.iLastTime.takeIf { it > 0 }

        val currentSectors = lapState.onFrame(
            currentSectorIndex = graphics.currentSectorIndex,
            lastSectorTimeMs = graphics.lastSectorTime,
            isValidLap = isValidLap,
        )

        if (completedLaps != lastCompletedLaps) {
            if (lastCompletedLaps != -1) {
                lastLapSectors = lapState.getLastCompletedLapSectors()
                
                if (isValidLap && lastLapTimeMs != null) {
                    bestValidLapTimeMs = bestValidLapTimeMs?.let { minOf(it, lastLapTimeMs) } ?: lastLapTimeMs
                }
                lapState.reset()
            }
            lastCompletedLaps = completedLaps
        }

        val sectorsToShow = if (currentSectors.all { it.timeMs == null } && lastLapSectors.isNotEmpty()) {
            lastLapSectors
        } else {
            currentSectors
        }

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

            sectors = sectorsToShow,
        )
    }
}
