package com.project.analyzer.telemetry.recording.impl.file.pipeline

import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession

internal interface FileTelemetryFrameWriteAdapter {

    fun onStageStart(stageName: String, session: ActiveSession, payload: TelemetryFramePayload) = Unit

    fun onStageStop(stageName: String, session: ActiveSession, payload: TelemetryFramePayload, sessionClosed: Boolean) =
        Unit

    fun onFrameWritten(session: ActiveSession, payload: TelemetryFramePayload) = Unit

    fun onFrameIoError(session: ActiveSession, payload: TelemetryFramePayload, error: Throwable) = Unit
}
