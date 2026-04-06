package com.project.analyzer.telemetry.recording.api.session

public data class DecodedRecordedTelemetryFrame(
    val segmentId: Long,
    val frameId: Long,
    val timestampNs: Long,
    val lapNumber: Int,
    val sectorIndex: Int,
    val trackPosition: Float? = null,
    val trackX: Float? = null,
    val trackY: Float? = null,
    val headingRad: Float? = null,
    val payload: RecordedTelemetryPayload,
    val flags: Int = 0,
)
