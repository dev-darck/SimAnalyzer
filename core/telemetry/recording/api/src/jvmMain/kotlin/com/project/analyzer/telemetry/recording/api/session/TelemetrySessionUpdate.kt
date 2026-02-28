package com.project.analyzer.telemetry.recording.api.session

public data class TelemetrySessionUpdate(
    val sessionId: Long,
    val gameId: String,
    val sessionType: String? = null,
    val carModel: String? = null,
    val carName: String? = null,
    val carId: Int? = null,
    val trackId: String? = null,
    val trackName: String? = null,
    val airTempC: Float? = null,
    val trackTempC: Float? = null,
    val dataSource: String? = null,
)
