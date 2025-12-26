package com.project.analyzer.ac.telemetry.impl.di

import com.project.analyzer.ac.telemetry.impl.AcTelemetryDataSourceImpl
import com.project.analyzer.telemetry.ac.api.TelemetryDataSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AcTelemetryDataSourceBinding(
    impl: AcTelemetryDataSourceImpl
) : TelemetryDataSource by impl
