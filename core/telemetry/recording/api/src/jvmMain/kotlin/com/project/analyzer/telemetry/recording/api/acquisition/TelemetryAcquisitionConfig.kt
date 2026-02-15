package com.project.analyzer.telemetry.recording.api.acquisition

public data class TelemetryAcquisitionConfig(
    val samplingRateHz: Int,
    val storageLocation: String,
    val recordingEnabled: Boolean,
    val maxRecordedLaps: Int,
)
