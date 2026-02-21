package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.lap.SectorFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleScoring
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleTelemetry

internal fun mapLap(telemetry: LmuVehicleTelemetry, scoring: LmuVehicleScoring?): LapFrame {
    val lapIndex = telemetry.lapNumber
    return LapFrame(
        currentLapIndex = lapIndex.takeIf { it > 0 },
        completedLaps = if (lapIndex > 0) lapIndex - 1 else null,
        currentLapTimeMs = lapTimeMs(telemetry.elapsedTime, telemetry.lapStartEt),
        lastLapTimeMs = scoring?.lastLapTime?.toLapMs(),
        bestLapTimeMs = scoring?.bestLapTime?.toLapMs(),
        currentSectorIndex = scoring?.sector?.takeIf { it >= 0 },
        sectors = buildList {
            scoring?.let { s ->
                add(SectorFrame(index = 0, timeMs = s.curSector1.toLapMs(), bestTimeMs = s.bestSector1.toLapMs()))
                add(SectorFrame(index = 1, timeMs = s.curSector2.toLapMs(), bestTimeMs = s.bestSector2.toLapMs()))
            }
        },
    )
}

private fun lapTimeMs(elapsed: Double, lapStartEt: Double): Int? {
    if (elapsed <= 0 || lapStartEt <= 0) return null
    val delta = elapsed - lapStartEt
    return if (delta >= 0) (delta * MS_IN_SECOND).toInt() else null
}

private fun Double.toLapMs(): Int? = if (this > 0.0) (this * MS_IN_SECOND).toInt() else null

private const val MS_IN_SECOND = 1000.0
