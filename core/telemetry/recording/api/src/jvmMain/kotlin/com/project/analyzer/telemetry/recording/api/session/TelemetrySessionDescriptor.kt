package com.project.analyzer.telemetry.recording.api.session

public data class TelemetrySessionDescriptor(
    val sessionId: Long,
    val gameId: String,
    val sessionGroupId: String? = null,
    val sessionType: String? = null,
    val carModel: String? = null,
    val carName: String? = null,
    val carId: Int? = null,
    val trackId: String? = null,
    val trackName: String? = null,
    val airTempC: Float? = null,
    val trackTempC: Float? = null,
    val startedAtMs: Long,
    val dataSource: String? = null,
    val payloadType: String = "",
    val payloadSize: Int = 0,
)
