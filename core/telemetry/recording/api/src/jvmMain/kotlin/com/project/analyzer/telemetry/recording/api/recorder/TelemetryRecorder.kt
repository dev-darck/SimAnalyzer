package com.project.analyzer.telemetry.recording.api.recorder

import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate

public interface TelemetryRecorder {

    public suspend fun startSession(descriptor: TelemetrySessionDescriptor)
    public suspend fun updateSession(update: TelemetrySessionUpdate)
    public suspend fun recordFrame(payload: TelemetryFramePayload)
    public suspend fun pauseSession(gameId: String, sessionId: Long, reason: String?)
    public suspend fun resumeSession(gameId: String, sessionId: Long)
    public suspend fun endSession(gameId: String, sessionId: Long, reason: String?)
    public suspend fun close()
}
