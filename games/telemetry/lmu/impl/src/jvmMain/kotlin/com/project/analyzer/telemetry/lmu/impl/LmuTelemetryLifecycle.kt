package com.project.analyzer.telemetry.lmu.impl

import com.project.analyzer.api.di.IO
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionField
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.LapFinished
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.LapStarted
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuTelemetrySnapshot
import com.project.analyzer.telemetry.lmu.impl.mapper.LmuTelemetryMapper
import com.project.analyzer.telemetry.lmu.impl.recording.LmuTelemetryRecordingEmitter
import com.project.analyzer.utils.logger
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

@Inject
internal class LmuTelemetryLifecycle(
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
    private val feed: LmuTelemetryFeed,
    private val mapper: LmuTelemetryMapper,
    private val recordingEmitter: LmuTelemetryRecordingEmitter,
) : TelemetryLifecycle {

    private var appScope = createScope()
    private var loopJob: Job? = null

    private var connected: Boolean = false
    private var sessionId: Long = 0L
    private var lastLapIndex: Int? = null
    private var currentSession: SessionInfo? = null

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

        if (connected) {
            emitSessionEnded(SessionEndReason.SIM_DISCONNECTED)
        }

        appScope.cancel()
        resetState()

        LeakCanaryRuntime.watch(this, "LmuTelemetryLifecycle")
    }

    private suspend fun listenLoop() {
        try {
            feed.collectFrames { snapshot ->
                onSnapshot(snapshot)
            }
        } finally {
            if (connected && currentCoroutineContext().isActive) {
                emitSessionEnded(SessionEndReason.SIM_DISCONNECTED)
            }
        }
    }

    private suspend fun onSnapshot(snapshot: LmuTelemetrySnapshot) {
        val frame = mapper.map(snapshot)
        if (!connected) {
            connected = true
            sessionId += 1L
            val session = SessionInfo(
                sessionId,
                SessionType.UNKNOWN,
                frame.session?.car?.carModel.orEmpty(),
                frame.session?.track?.trackId.orEmpty(),
            )
            currentSession = session
            _events.emit(TelemetryLifecycleEvent.SimConnected)
            _events.emit(TelemetryLifecycleEvent.SessionStarted(session))
        }

        processLapIndex(frame)
        updateSessionFromFrame(frame)
        _frames.emit(frame)
        emitSampleIfNeeded(snapshot, frame)
    }

    private suspend fun processLapIndex(frame: TelemetryFrame) {
        val newLapIndex = frame.lap?.currentLapIndex
        val prevLap = lastLapIndex

        when {
            newLapIndex != null && prevLap == null -> {
                _events.emit(LapStarted(newLapIndex))
            }

            newLapIndex != null && prevLap != null && newLapIndex != prevLap -> {
                _events.emit(LapFinished(prevLap, LapValidity.UNKNOWN))
                _events.emit(LapStarted(newLapIndex))
            }
        }

        lastLapIndex = newLapIndex
    }

    private suspend fun emitSessionEnded(reason: SessionEndReason) {
        if (sessionId > 0L) {
            _events.emit(TelemetryLifecycleEvent.SessionEnded(sessionId, reason))
        }
        _events.emit(TelemetryLifecycleEvent.SimDisconnected)
        resetState()
    }

    private suspend fun updateSessionFromFrame(frame: TelemetryFrame) {
        val cur = currentSession ?: return
        var updated = cur
        val changed = linkedSetOf<SessionField>()

        val newCar = frame.session?.car?.carModel.orEmpty().trim()
        if (newCar.isNotBlank() && newCar != cur.carModel) {
            updated = updated.copy(carModel = newCar)
            changed += SessionField.CAR_MODEL
        }

        val newTrack = frame.session?.track?.trackId.orEmpty().trim()
        if (newTrack.isNotBlank() && newTrack != cur.trackId) {
            updated = updated.copy(trackId = newTrack)
            changed += SessionField.TRACK_ID
        }

        if (changed.isNotEmpty()) {
            currentSession = updated
            _events.emit(TelemetryLifecycleEvent.SessionUpdated(updated, changed.toSet()))
        }
    }

    private suspend fun emitSampleIfNeeded(snapshot: LmuTelemetrySnapshot, frame: TelemetryFrame) {
        if (!connected || sessionId <= 0L) return
        recordingEmitter.emitSample(sessionId, snapshot, frame)
    }

    private fun resetState() {
        connected = false
        lastLapIndex = null
        currentSession = null
    }

    private fun createScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + ioDispatcher + CoroutineExceptionHandler { _, e ->
            logger.error(e) { "[lmu] lifecycle uncaught exception" }
        },
    )
}
