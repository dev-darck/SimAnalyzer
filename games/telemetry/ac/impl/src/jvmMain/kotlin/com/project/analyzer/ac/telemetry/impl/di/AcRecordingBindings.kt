package com.project.analyzer.ac.telemetry.impl.di

import com.project.analyzer.ac.telemetry.impl.recording.AcTelemetryRecordingEmitter
import com.project.analyzer.ac.telemetry.impl.recording.AcTelemetryRecordingSource
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSource
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet

@ContributesTo(SessionScope::class)
@BindingContainer
interface AcRecordingBindings {

    @Binds
    fun bindRecordingEmitter(impl: AcTelemetryRecordingSource): AcTelemetryRecordingEmitter

    @Binds
    @IntoSet
    fun bindRecordingSource(impl: AcTelemetryRecordingSource): TelemetryRecordingSource
}
