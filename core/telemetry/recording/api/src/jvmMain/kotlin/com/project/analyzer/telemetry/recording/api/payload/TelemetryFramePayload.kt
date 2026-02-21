package com.project.analyzer.telemetry.recording.api.payload

import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndex

public data class TelemetryFramePayload(
    val sessionId: Long,
    val gameId: String,
    val timestampNs: Long,
    val frameId: Long,
    val payloadType: String,
    val dataSourceId: Int,
    val payload: ByteArray,
    val index: TelemetryFrameIndex? = null,
)
