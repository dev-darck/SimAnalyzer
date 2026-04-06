package com.project.analyzer.telemetry.recording.api.session

public data class DecodedRecordedTelemetrySegment(
    val segmentId: Long,
    val sessionType: String?,
    val carModel: String?,
    val carName: String?,
    val trackId: String?,
    val trackName: String?,
    val startedAtMs: Long,
    val endedAtMs: Long?,
)
