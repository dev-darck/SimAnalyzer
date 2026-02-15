package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSample

internal sealed interface TelemetryRecordingInput {
    data class TelemetryRecordingConfigInput(
        val config: TelemetryAcquisitionConfig
    ) : TelemetryRecordingInput

    data class TelemetryRecordingEventInput(
        val event: TelemetryLifecycleEvent
    ) : TelemetryRecordingInput

    data class TelemetryRecordingSampleInput(
        val sample: TelemetryRecordingSample
    ) : TelemetryRecordingInput
}
