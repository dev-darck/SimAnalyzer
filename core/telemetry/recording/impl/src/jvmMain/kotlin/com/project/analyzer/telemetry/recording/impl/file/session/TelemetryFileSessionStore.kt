package com.project.analyzer.telemetry.recording.impl.file.session

import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.model.SessionKey

internal interface TelemetryFileSessionStore {
    fun keyFor(gameId: String, sessionId: Long): SessionKey
    fun openSession(descriptor: TelemetrySessionDescriptor, config: TelemetryAcquisitionConfig): ActiveSession?
    fun pauseSession(session: ActiveSession, reason: String?)
    fun resumeSession(session: ActiveSession)
    fun applyUpdate(session: ActiveSession, update: TelemetrySessionUpdate)
    fun writeFrame(session: ActiveSession, payload: TelemetryFramePayload): Boolean
    fun closeSession(session: ActiveSession, endReason: String?): Boolean
}
