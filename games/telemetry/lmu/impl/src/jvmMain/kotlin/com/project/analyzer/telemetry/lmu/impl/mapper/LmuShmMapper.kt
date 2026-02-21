package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.lmu.api.model.LmuTelemetrySnapshot
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2ScoringInfo
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleScoring
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleTelemetry
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
internal class LmuShmMapper {

    fun map(
        frameId: Long,
        telemetryVersion: Int,
        scoringVersion: Int?,
        numVehicles: Int,
        playerIndex: Int,
        telemetry: Rf2VehicleTelemetry,
        scoring: Rf2VehicleScoring?,
        scoringInfo: Rf2ScoringInfo?,
    ): LmuTelemetrySnapshot = LmuTelemetrySnapshot(
        frameId = frameId,
        telemetryVersion = telemetryVersion,
        scoringVersion = scoringVersion,
        numVehicles = numVehicles,
        playerIndex = playerIndex,
        telemetry = telemetry.toModel(),
        scoring = scoring?.toModel(),
        scoringInfo = scoringInfo?.toModel(),
        timestampNs = System.nanoTime(),
    )
}
