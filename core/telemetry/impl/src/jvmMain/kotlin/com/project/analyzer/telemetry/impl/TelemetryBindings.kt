package com.project.analyzer.telemetry.impl

import com.project.analyzer.telemetry.api.contract.TelemetryEventSource
import com.project.analyzer.telemetry.api.contract.TelemetryFrameSource
import com.project.analyzer.telemetry.api.contract.TelemetryRuntimeController
import com.project.analyzer.telemetry.api.contract.TelemetryReadSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
interface TelemetryBindings {
    companion object {

        @Provides
        @SingleIn(AppScope::class)
        private fun provideTelemetryFrameSource(impl: TelemetryLifecycleRouter): TelemetryFrameSource = impl

        @Provides
        @SingleIn(AppScope::class)
        private fun provideTelemetryEventSource(impl: TelemetryLifecycleRouter): TelemetryEventSource = impl

        @Provides
        @SingleIn(AppScope::class)
        private fun provideTelemetryReadSource(impl: TelemetryLifecycleRouter): TelemetryReadSource = impl

        @Provides
        @SingleIn(AppScope::class)
        private fun provideTelemetryRuntimeController(impl: TelemetryLifecycleRouter): TelemetryRuntimeController = impl
    }
}
