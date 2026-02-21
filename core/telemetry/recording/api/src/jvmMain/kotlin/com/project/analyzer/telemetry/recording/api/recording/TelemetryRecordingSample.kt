package com.project.analyzer.telemetry.recording.api.recording

import com.project.analyzer.telemetry.api.model.TelemetryFrame

public data class TelemetryRecordingSample(
    val sessionId: Long,
    val timestampNs: Long,
    val frameId: Long,
    val gameId: String,
    val dataSourceId: Int,
    val dataSource: String?,
    val payloadType: String,
    val payload: ByteArray,
    val frame: TelemetryFrame,
)
