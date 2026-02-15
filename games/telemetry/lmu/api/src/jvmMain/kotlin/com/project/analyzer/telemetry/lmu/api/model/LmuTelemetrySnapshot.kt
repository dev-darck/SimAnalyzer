package com.project.analyzer.telemetry.lmu.api.model

public data class LmuTelemetrySnapshot(
    val frameId: Long,
    val telemetryVersion: Int,
    val scoringVersion: Int?,
    val numVehicles: Int,
    val playerIndex: Int,
    val telemetry: LmuVehicleTelemetry,
    val scoring: LmuVehicleScoring?,
    val scoringInfo: LmuScoringInfo?,
    val timestampNs: Long
)
