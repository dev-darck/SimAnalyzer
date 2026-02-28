package com.project.analyzer.telemetry.recording.impl.file.session.pipeline

import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession

internal class FrameWriteContext(val session: ActiveSession, val payload: TelemetryFramePayload) {
    var storagePayloadType: String? = null
    var storagePayloadBytes: ByteArray? = null
    var storagePayloadSize: Int? = null
    var shouldEnqueueCompression: Boolean = false
}
