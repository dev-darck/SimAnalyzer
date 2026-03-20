package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.api.contract.TelemetryEventSource
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingController
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSource
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingInput.TelemetryRecordingConfigInput
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingInput.TelemetryRecordingEventInput
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingInput.TelemetryRecordingSampleInput
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Recording orchestrator.
 *
 * Merges config + telemetry events + samples into a single input stream and delegates session/write logic
 * to [TelemetryRecordingSessionCoordinator].
 */
@Inject
@SingleIn(SessionScope::class)
internal class TelemetryRecordingControllerImpl(
    private val telemetryEvents: TelemetryEventSource,
    private val sources: Set<@JvmSuppressWildcards TelemetryRecordingSource>,
    private val settings: TelemetryAcquisitionSettings,
    private val coordinator: TelemetryRecordingSessionCoordinator,
    private val tempSessionCleanup: TelemetryRecordingUnsavedSessionCleanup,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryRecordingController {

    private val logger = logger()
    private val controllerDispatcher: CoroutineDispatcher =
        ioDispatcher.limitedParallelism(2, "TelemetryRecordingController")
    private val scope = CoroutineScope(
        SupervisorJob() + controllerDispatcher + CoroutineExceptionHandler { _, e ->
            logger.error(e) { "[recording] recording controller failed" }
        },
    )

    private var job: Job? = null
    private var startupCleanupJob: Job? = null

    override suspend fun start() {
        if (job?.isActive == true) return
        startupCleanupJob?.cancelAndJoin()

        job = scope.launch {
            inputFlow().collect(coordinator::handle)
        }
        startupCleanupJob = scope.launch {
            tempSessionCleanup.cleanup(reason = "app_start")
        }
    }

    override suspend fun stop() {
        startupCleanupJob?.cancelAndJoin()
        startupCleanupJob = null
        job?.cancelAndJoin()
        job = null

        coordinator.shutdownAndCloseRecorder()
        sources.forEach { it.close() }

        tempSessionCleanup.cleanup(reason = "app_stop")
        scope.cancel()

        LeakCanaryRuntime.watch(this, "TelemetryRecordingController")
    }

    private fun inputFlow(): Flow<TelemetryRecordingInput> {
        val inputs = mutableListOf<Flow<TelemetryRecordingInput>>()
        inputs += settings.observeConfig().map { TelemetryRecordingConfigInput(it) }
        inputs += telemetryEvents.events.map { TelemetryRecordingEventInput(it) }
        sources.forEach { source ->
            inputs += source.samples.map { TelemetryRecordingSampleInput(it) }
        }
        return inputs.merge()
    }
}
