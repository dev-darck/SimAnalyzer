package com.project.analyzer.telemetry.recording.api.session

public data class RecordedTelemetrySessionBundle(
    val sessionId: Long,
    val metadata: RecordedTelemetrySessionMetadata,
    val locations: List<RecordedTelemetrySessionLocation>,
)
