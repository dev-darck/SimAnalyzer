package com.project.analyzer.telemetry.recording.api.session

import java.io.File

public data class RecordedTelemetrySessionLocation(
    val persistedSessionId: Long,
    val dir: File,
    val metadata: RecordedTelemetrySessionMetadata,
)
