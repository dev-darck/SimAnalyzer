package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.internal.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.AcPollPipeline
import com.project.analyzer.ac.telemetry.impl.internal.AcRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.DataSourceType
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.PollResult
import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcMapper
import com.project.analyzer.ac.telemetry.impl.recording.AcTelemetryRecordingEmitter
import com.project.analyzer.api.di.IO
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * AC lifecycle orchestrator.
 *
 * Pipeline:
 * poll -> map(raw snapshot -> frame) -> sessionTracker -> recording emit -> emit streams.
 */
@Inject
class AcTelemetryLifecycle internal constructor(
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
    private val mapper: AcMapper,
    private val recordingEmitter: AcTelemetryRecordingEmitter,
    private val pollPipeline: AcPollPipeline,
) : TelemetryLifecycle {

    private val logger = logger()
    private val lifecycleDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(1, "AcTelemetryLifecycle")

    private var appScope = createScope()
    private var processingJob: Job? = null

    private val _events = MutableSharedFlow<TelemetryLifecycleEvent>(
        replay = 1,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events = _events.asSharedFlow()

    private val _frames: MutableSharedFlow<TelemetryFrame> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val frames: SharedFlow<TelemetryFrame> = _frames.asSharedFlow()

    private val sessionTracker = AcSessionTracker()
    private var lastDataSource: DataSourceType = DataSourceType.NATIVE

    internal constructor(
        pollLoop: AcPollLoop,
        fallback: AcEvoFallbackShmPatcher,
        mapper: AcMapper,
        recordingEmitter: AcTelemetryRecordingEmitter,
        ioDispatcher: CoroutineDispatcher,
    ) : this(
        mapper = mapper,
        recordingEmitter = recordingEmitter,
        ioDispatcher = ioDispatcher,
        pollPipeline = AcPollPipeline(pollLoop = pollLoop, fallback = fallback),
    )

    override suspend fun launchTelemetry() {
        if (processingJob?.isActive == true) return
        if (!appScope.isActive) {
            appScope = createScope()
        }
        launchLoop()
    }

    override suspend fun finishTelemetry() {
        processingJob?.cancelAndJoin()
        processingJob = null
        pollPipeline.stop()
        appScope.cancel()
        resetState()

        LeakCanaryRuntime.watch(this, "AcTelemetryLifecycle")
    }

    private fun launchLoop() {
        val channel = pollPipeline.start(appScope)
        processingJob = appScope.launch {
            processResults(channel)
        }
    }

    private suspend fun processResults(channel: Channel<PollResult>) {
        var connectionState: GameConnectionState = GameConnectionState.DISCONNECTED

        for (result in channel) {
            when (result) {
                is PollResult.StateChanged -> {
                    connectionState = result.state
                    lastDataSource = result.dataSource
                    runCatching {
                        sessionTracker.onConnectionStateChanged(result.state, result.dataSource) { event ->
                            _events.tryEmit(event)
                        }
                    }.onFailure { error ->
                        logger.atError(RATE_LIMITED) {
                            message = "state processing error state=${result.state}"
                            cause = error
                        }
                    }
                }

                is PollResult.Frame -> {
                    val snapshot = result.snapshot
                    runCatching {
                        if (connectionState == GameConnectionState.IN_SESSION) {
                            val rawFrame = mapper.map(snapshot)
                            val frame = processFrame(snapshot, rawFrame, connectionState) ?: rawFrame
                            _frames.tryEmit(frame)
                        }
                    }.onFailure { error ->
                        logger.atError(RATE_LIMITED) {
                            message = "frame processing error"
                            cause = error
                        }
                    }
                    pollPipeline.release(snapshot)
                }
            }
        }
    }

    private fun createScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + lifecycleDispatcher + CoroutineExceptionHandler { _, e ->
            logger.error(e) { "uncaught exception" }
        },
    )

    private suspend fun processFrame(
        snapshot: AcRawSnapshot,
        frame: TelemetryFrame,
        state: GameConnectionState,
    ): TelemetryFrame? {
        if (state != GameConnectionState.IN_SESSION) return null

        val result = sessionTracker.onFrame(frame, lastDataSource, emit = { event ->
            _events.tryEmit(event)
        }, restartHint = snapshot.sessionRestartHint)
        emitSampleIfNeeded(snapshot, result)
        return result.frame
    }

    private suspend fun emitSampleIfNeeded(snapshot: AcRawSnapshot, result: AcLifecycleFrameResult) {
        val sampleSessionId = result.sampleSessionId ?: return
        recordingEmitter.emitSample(
            sessionId = sampleSessionId,
            snapshot = snapshot,
            frame = result.frame,
            dataSource = lastDataSource,
        )
    }

    private fun resetState() {
        lastDataSource = DataSourceType.NATIVE
        sessionTracker.resetAll()
    }
}
