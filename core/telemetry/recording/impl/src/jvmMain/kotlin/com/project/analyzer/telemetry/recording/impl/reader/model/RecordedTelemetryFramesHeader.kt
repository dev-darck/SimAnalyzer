package com.project.analyzer.telemetry.recording.impl.reader.model

internal data class RecordedTelemetryFramesHeader(
    val storagePayloadType: String,
    val storagePayloadSize: Int,
)
