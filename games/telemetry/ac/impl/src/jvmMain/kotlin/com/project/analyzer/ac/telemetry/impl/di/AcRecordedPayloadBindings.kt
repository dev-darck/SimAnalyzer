package com.project.analyzer.ac.telemetry.impl.di

import com.project.analyzer.ac.telemetry.impl.recording.analysis.AcRecordedPayloadDecoder
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayloadDecoder
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
interface AcRecordedPayloadBindings {

    companion object {

        @Provides
        @IntoSet
        private fun provideRecordedPayloadDecoder(
            impl: AcRecordedPayloadDecoder,
        ): RecordedTelemetryPayloadDecoder = impl
    }
}
