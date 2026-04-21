package com.project.analyzer.ac.telemetry.impl.internal.mapper.ace

import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import dev.zacsweers.metro.Inject

@Inject
internal class AcEvoLapMapper {

    fun enrich(base: LapFrame?, snapshot: AceRawSnapshot): LapFrame {
        val graphics = snapshot.graphics
        val timing = graphics.timingState
        val sessionState = graphics.sessionState
        val baseLap = base ?: LapFrame()

        val currentLapIndex = sessionState.currentLap.takeIf { it > 0 }
            ?: baseLap.currentLapIndex
            ?: (graphics.totalLapCount + 1).takeIf { it > 0 }

        val validity = when {
            timing.isInvalid -> LapValidity.INVALID
            else -> baseLap.validity
        }

        val isDeltaPositive = when {
            graphics.deltaTimeMs != 0 -> graphics.deltaTimeMs > 0
            timing.deltaCurrentP != 0 -> timing.deltaCurrentP > 0
            else -> baseLap.isDeltaPositive
        }

        return baseLap.copy(
            currentLapIndex = currentLapIndex,
            completedLaps = graphics.totalLapCount.takeIf { it >= 0 } ?: baseLap.completedLaps,
            currentLapTimeMs = graphics.currentLapTimeMs.takeIf { it >= 0 } ?: baseLap.currentLapTimeMs,
            lastLapTimeMs = graphics.lastLaptimeMs.takeIf { it > 0 } ?: baseLap.lastLapTimeMs,
            bestLapTimeMs = graphics.bestLaptimeMs.takeIf { it > 0 } ?: baseLap.bestLapTimeMs,
            currentLapTimeLabel = timing.currentLaptime.ifBlank { baseLap.currentLapTimeLabel },
            lastLapTimeLabel = timing.lastLaptime.ifBlank { baseLap.lastLapTimeLabel },
            bestLapTimeLabel = timing.bestLaptime.ifBlank { baseLap.bestLapTimeLabel },
            deltaLapTimeMs = graphics.deltaTimeMs.takeIf { it != 0 } ?: baseLap.deltaLapTimeMs,
            isDeltaPositive = isDeltaPositive,
            estimatedLapTimeMs = graphics.predictedLapTimeMs.takeIf { it > 0 } ?: baseLap.estimatedLapTimeMs,
            deltaLapTimeLabel = timing.deltaCurrent.ifBlank { baseLap.deltaLapTimeLabel },
            deltaLastLapTimeLabel = timing.deltaLast.ifBlank { baseLap.deltaLastLapTimeLabel },
            isDeltaLastPositive = when {
                timing.deltaLastP != 0 -> timing.deltaLastP > 0
                else -> baseLap.isDeltaLastPositive
            },
            estimatedLapTimeLabel = timing.idealLaptime.ifBlank { baseLap.estimatedLapTimeLabel },
            totalTimeLabel = timing.totalTime.ifBlank { baseLap.totalTimeLabel },
            validity = validity,
        )
    }
}
