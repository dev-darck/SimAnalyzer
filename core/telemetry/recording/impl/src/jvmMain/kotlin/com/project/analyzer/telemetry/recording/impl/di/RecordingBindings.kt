package com.project.analyzer.telemetry.recording.impl.di

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.recording.api.recorder.TelemetryRecorder
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingController
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingControllerImpl
import com.project.analyzer.telemetry.recording.impl.file.FileTelemetryRecorder
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(SessionScope::class)
@BindingContainer
interface RecordingBindings {

    @Binds
    fun bindRecordingController(impl: TelemetryRecordingControllerImpl): TelemetryRecordingController

    @Binds
    fun bindTelemetryRecorder(impl: FileTelemetryRecorder): TelemetryRecorder
}
