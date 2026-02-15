package com.project.analyzer.telemetry.lmu.impl.di

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.lmu.impl.recording.LmuTelemetryRecordingEmitter
import com.project.analyzer.telemetry.lmu.impl.recording.LmuTelemetryRecordingSource
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSource
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(SessionScope::class)
@BindingContainer
object LmuRecordingBindings {

    @Provides
    private fun provideRecordingEmitter(
        impl: LmuTelemetryRecordingSource
    ): LmuTelemetryRecordingEmitter = impl

    @Provides
    @IntoSet
    private fun provideRecordingSource(
        impl: LmuTelemetryRecordingSource
    ): TelemetryRecordingSource = impl
}
