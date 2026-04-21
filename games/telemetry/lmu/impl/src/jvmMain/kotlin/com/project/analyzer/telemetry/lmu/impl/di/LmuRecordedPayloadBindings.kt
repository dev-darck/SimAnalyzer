package com.project.analyzer.telemetry.lmu.impl.di

import com.project.analyzer.telemetry.lmu.impl.recording.analysis.LmuRecordedPayloadDecoder
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayloadDecoder
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
interface LmuRecordedPayloadBindings {

    companion object {

        @Provides
        @IntoSet
        private fun provideRecordedPayloadDecoder(impl: LmuRecordedPayloadDecoder): RecordedTelemetryPayloadDecoder =
            impl
    }
}
