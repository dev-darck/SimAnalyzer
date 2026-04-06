package com.project.analyzer.telemetry.recording.api.session

public data class RecordedTelemetryTyreSnapshot(
    val pressurePsi: Float? = null,
    val coreTempC: Float? = null,
    val innerTempC: Float? = null,
    val middleTempC: Float? = null,
    val outerTempC: Float? = null,
    val avgTempC: Float? = null,
    val brakeTempC: Float? = null,
    val slip: Float? = null,
    val load: Float? = null,
)
