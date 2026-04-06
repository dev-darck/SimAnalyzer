package com.project.analyzer.telemetry.recording.api.session

public data class DecodedRecordedTelemetrySession(
    val sessionId: Long,
    val metadata: RecordedTelemetrySessionMetadata,
    val segments: List<DecodedRecordedTelemetrySegment>,
    val frames: List<DecodedRecordedTelemetryFrame>,
)
