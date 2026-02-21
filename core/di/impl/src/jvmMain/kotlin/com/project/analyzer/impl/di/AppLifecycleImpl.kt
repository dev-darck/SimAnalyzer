package com.project.analyzer.impl.di

import com.project.analyzer.api.di.AppLifecycle
import com.project.analyzer.api.di.IO
import com.project.analyzer.leak.api.LeakCanaryController
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<AppLifecycle>())
class AppLifecycleImpl(
    private val telemetryLifecycle: TelemetryLifecycle,
    private val telemetryRecordingController: TelemetryRecordingController,
    private val leakCanaryController: LeakCanaryController,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : AppLifecycle {

    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher + CoroutineExceptionHandler { _, _ -> })
    private var startupJob: Job? = null

    override suspend fun start() {
        if (!started.compareAndSet(false, true)) return
        LeakCanaryRuntime.install(leakCanaryController)
        startupJob?.cancelAndJoin()
        startupJob = scope.launch {
            coroutineScope {
                launch {
                    runCatching { telemetryLifecycle.launchTelemetry() }
                }
                launch {
                    runCatching { telemetryRecordingController.start() }
                }
                launch {
                    runCatching { leakCanaryController.start() }
                }
            }
        }
    }

    override suspend fun stop() {
        if (!started.compareAndSet(true, false)) return
        startupJob?.cancelAndJoin()
        startupJob = null

        coroutineScope {
            launch {
                runCatching { telemetryRecordingController.stop() }
            }
            launch {
                runCatching { telemetryLifecycle.finishTelemetry() }
            }
            launch {
                runCatching { leakCanaryController.stop() }
            }
        }
        LeakCanaryRuntime.uninstall(leakCanaryController)
    }
}
