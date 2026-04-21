package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.lap.SectorFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleScoring
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleTelemetry
import com.project.analyzer.telemetry.lmu.impl.common.LmuTelemetryConversions

internal fun mapLap(telemetry: LmuVehicleTelemetry, scoring: LmuVehicleScoring?): LapFrame {
    val completedLaps = telemetry.lapNumber.takeIf { it >= 0 }
    val currentSectorIndex = scoring?.sector?.let(LmuTelemetryConversions::commonSectorIndex)
        ?: LmuTelemetryConversions.commonSectorIndex(telemetry.currentSector)
    return LapFrame(
        currentLapIndex = completedLaps?.let { it + 1 },
        completedLaps = completedLaps,
        currentLapTimeMs = LmuTelemetryConversions.currentLapTimeMs(
            currentEt = telemetry.elapsedTime,
            lapStartEt = scoring?.lapStartEt ?: telemetry.lapStartEt,
        ),
        lastLapTimeMs = scoring?.lastLapTime?.let(LmuTelemetryConversions::lapSecondsToMs),
        bestLapTimeMs = scoring?.bestLapTime?.let(LmuTelemetryConversions::lapSecondsToMs),
        sectorCount = SECTOR_COUNT,
        currentSectorIndex = currentSectorIndex,
        lastSectorTimeMs = scoring?.lastCompletedSectorTimeMs(),
        validity = LapValidity.fromBoolean(!telemetry.lapInvalidated),
        sectors = buildList {
            scoring?.let { s ->
                add(
                    SectorFrame(
                        index = 0,
                        timeMs = LmuTelemetryConversions.lapSecondsToMs(s.curSector1),
                        bestTimeMs = LmuTelemetryConversions.lapSecondsToMs(s.bestSector1),
                    ),
                )
                add(
                    SectorFrame(
                        index = 1,
                        timeMs = LmuTelemetryConversions.lapSecondsToMs(s.curSector2),
                        bestTimeMs = LmuTelemetryConversions.lapSecondsToMs(s.bestSector2),
                    ),
                )
            }
        },
    )
}

private fun LmuVehicleScoring.lastCompletedSectorTimeMs(): Int? =
    when (LmuTelemetryConversions.commonSectorIndex(sector)) {
        0 -> lastSector3TimeSeconds()
        1 -> curSector1
        2 -> curSector2
        else -> null
    }?.let(LmuTelemetryConversions::lapSecondsToMs)

private fun LmuVehicleScoring.lastSector3TimeSeconds(): Double? {
    if (lastLapTime <= 0.0 || lastSector1 <= 0.0 || lastSector2 <= 0.0) return null
    return (lastLapTime - lastSector1 - lastSector2).takeIf { it > 0.0 }
}

private const val SECTOR_COUNT = 3
