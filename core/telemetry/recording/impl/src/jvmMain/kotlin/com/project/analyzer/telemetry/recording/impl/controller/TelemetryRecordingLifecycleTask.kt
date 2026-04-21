package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.api.di.AppLifecycleTask
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@ContributesIntoSet(AppScope::class)
@SingleIn(AppScope::class)
class TelemetryRecordingLifecycleTask(private val telemetryRecordingController: TelemetryRecordingController) :
    AppLifecycleTask {

    override val startOrder: Int = 30
    override val stopOrder: Int = 10

    override suspend fun start() {
        telemetryRecordingController.start()
    }

    override suspend fun stop() {
        telemetryRecordingController.stop()
    }
}
