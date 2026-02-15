package com.project.analyzer.telemetry.recording.impl.file.command

import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate

internal sealed interface RecordCommand {

    data object Close : RecordCommand

    data class End(
        val gameId: String,
        val sessionId: Long,
        val reason: String?
    ) : RecordCommand

    data class Frame(
        val payload: TelemetryFramePayload
    ) : RecordCommand

    data class Pause(
        val gameId: String,
        val sessionId: Long,
        val reason: String?
    ) : RecordCommand

    data class Resume(
        val gameId: String,
        val sessionId: Long
    ) : RecordCommand

    data class Start(
        val descriptor: TelemetrySessionDescriptor,
        val config: TelemetryAcquisitionConfig
    ) : RecordCommand

    data class Update(
        val update: TelemetrySessionUpdate
    ) : RecordCommand
}
