package com.project.analyzer.impl.di

import com.project.analyzer.api.di.AppLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.util.concurrent.atomic.AtomicBoolean

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<AppLifecycle>())
class AppLifecycleImpl(
    private val telemetryLifecycle: TelemetryLifecycle,
    private val telemetryRecordingController: TelemetryRecordingController,
) : AppLifecycle {

    private val started = AtomicBoolean(false)

    override suspend fun start() {
        if (!started.compareAndSet(false, true)) return
        telemetryLifecycle.launchTelemetry()
        telemetryRecordingController.start()
    }

    override suspend fun stop() {
        if (!started.compareAndSet(true, false)) return
        telemetryRecordingController.stop()
        telemetryLifecycle.finishTelemetry()
    }
}
