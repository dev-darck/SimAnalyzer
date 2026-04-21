package com.project.analyzer.telemetry.impl

import com.project.analyzer.api.di.AppLifecycleTask
import com.project.analyzer.telemetry.api.contract.TelemetryRuntimeController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@ContributesIntoSet(AppScope::class)
@SingleIn(AppScope::class)
class TelemetryRuntimeLifecycleTask(private val telemetryRuntimeController: TelemetryRuntimeController) :
    AppLifecycleTask {

    override val startOrder: Int = 20
    override val stopOrder: Int = 20

    override suspend fun start() {
        telemetryRuntimeController.launchTelemetry()
    }

    override suspend fun stop() {
        telemetryRuntimeController.finishTelemetry()
    }
}
