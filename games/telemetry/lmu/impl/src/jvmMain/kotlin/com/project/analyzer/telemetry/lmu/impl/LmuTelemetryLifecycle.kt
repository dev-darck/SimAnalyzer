package com.project.analyzer.telemetry.lmu.impl

import com.project.analyzer.api.di.IO
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuTelemetrySnapshot
import com.project.analyzer.telemetry.lmu.impl.mapper.LmuTelemetryMapper
import com.project.analyzer.telemetry.lmu.impl.recording.LmuTelemetryRecordingEmitter
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * LMU lifecycle orchestrator.
 *
 * Pipeline:
 * feed -> map -> sessionTracker -> recording side effect -> emit streams.
 */
@Inject
internal class LmuTelemetryLifecycle(
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
    private val feed: LmuTelemetryFeed,
    private val mapper: LmuTelemetryMapper,
    private val recordingEmitter: LmuTelemetryRecordingEmitter,
) : TelemetryLifecycle {
    private val logger = logger()
    private val lifecycleDispatcher: CoroutineDispatcher =
        ioDispatcher.limitedParallelism(1, "LmuTelemetryLifecycle")

    private var appScope = createScope()
    private var loopJob: Job? = null

    private val _events = MutableSharedFlow<TelemetryLifecycleEvent>(
        replay = 1,
        extraBufferCapacity = 32,
    )
    override val events = _events.asSharedFlow()

    private val _frames = MutableSharedFlow<TelemetryFrame>(
        replay = 1,
        extraBufferCapacity = 16,
    )
    override val frames: SharedFlow<TelemetryFrame> = _frames.asSharedFlow()

    private val sessionTracker = LmuSessionTracker()

    override suspend fun launchTelemetry() {
        if (loopJob?.isActive == true) return
        if (!appScope.isActive) {
            appScope = createScope()
        }
        loopJob = appScope.launch {
            listenLoop()
        }
    }

    override suspend fun finishTelemetry() {
        feed.close()
        loopJob?.cancelAndJoin()
        loopJob = null

        if (sessionTracker.isConnected) {
            sessionTracker.onDisconnected(SessionEndReason.SIM_DISCONNECTED, ::emitEvent)
        }

        appScope.cancel()
        sessionTracker.reset()

        LeakCanaryRuntime.watch(this, "LmuTelemetryLifecycle")
    }

    private suspend fun listenLoop() {
        try {
            feed.collectFrames { snapshot ->
                onSnapshot(snapshot)
            }
        } finally {
            if (sessionTracker.isConnected && currentCoroutineContext().isActive) {
                sessionTracker.onDisconnected(SessionEndReason.SIM_DISCONNECTED, ::emitEvent)
            }
        }
    }

    private suspend fun onSnapshot(snapshot: LmuTelemetrySnapshot) {
        val frame = mapper.map(snapshot)
        val result = sessionTracker.onFrame(frame, ::emitEvent)
        _frames.emit(result.frame)
        emitSampleIfNeeded(snapshot, result)
    }

    private suspend fun emitSampleIfNeeded(snapshot: LmuTelemetrySnapshot, result: LmuLifecycleFrameResult) {
        val sampleSessionId = result.sampleSessionId ?: return
        recordingEmitter.emitSample(sampleSessionId, snapshot, result.frame)
    }

    private suspend fun emitEvent(event: TelemetryLifecycleEvent) {
        _events.emit(event)
    }

    private fun createScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + lifecycleDispatcher + CoroutineExceptionHandler { _, e ->
            logger.error(e) { "[lmu] lifecycle uncaught exception" }
        },
    )
}
