package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.api.di.AppLifecycleTask
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@ContributesIntoSet(AppScope::class)
@SingleIn(AppScope::class)
class TrackCalibrationBootstrapTask(
    private val bootstrapper: TrackCalibrationBootstrapper,
) : AppLifecycleTask {

    override val startOrder: Int = 10

    override suspend fun start() {
        bootstrapper.ensureBundledCalibrationsInstalled()
    }
}
