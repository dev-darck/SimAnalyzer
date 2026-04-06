package com.project.analyzer.telemetry.recording.impl.reader.model

internal data class RecordedTelemetryFrameRecord(
    val timestampNs: Long,
    val frameId: Long,
    val dataSourceId: Int,
    val payload: ByteArray,
)
