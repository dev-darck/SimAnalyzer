package com.project.analyzer.telemetry.analysis.impl.di

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.analysis.api.service.RecordedTelemetryAnalysisService
import com.project.analyzer.telemetry.analysis.impl.service.RecordedTelemetryAnalysisServiceImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Exposes the analysis implementation graph so callers can resolve one cohesive recorded-session pipeline.
 */
@ContributesTo(ScreenScope::class)
@BindingContainer
interface SessionAnalysisCoreBindings {

    companion object {

        @Provides
        private fun provideRecordedTelemetryAnalysisService(
            impl: RecordedTelemetryAnalysisServiceImpl,
        ): RecordedTelemetryAnalysisService = impl
    }
}
