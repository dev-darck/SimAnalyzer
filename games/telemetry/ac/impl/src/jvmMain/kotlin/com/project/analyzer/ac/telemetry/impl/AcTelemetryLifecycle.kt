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
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionField
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionPauseReason
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.LapFinished
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.LapStarted
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

@Inject
class AcTelemetryLifecycle(
    private val mapper: AcMapper,
    private val recordingEmitter: AcTelemetryRecordingEmitter,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
    pollLoop: AcPollLoop,
    fallback: AcEvoFallbackShmPatcher,
) : TelemetryLifecycle {

    private val logger = logger()

    private var appScope = createScope()
    private var processingJob: Job? = null

    private val pollPipeline = AcPollPipeline(pollLoop = pollLoop, fallback = fallback)

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

    private var lastLapIndex: Int? = null
    private var lastLapValidity: LapValidity = LapValidity.UNKNOWN
    private var lastConnectionState: GameConnectionState = GameConnectionState.DISCONNECTED
    private var lastDataSource: DataSourceType = DataSourceType.NATIVE

    private var lastSessionIndex: Int? = null
    private var lastCompletedLaps: Int? = null
    private var lastSessionTimeLeftSec: Float? = null
    private var sessionState: SessionState = SessionState.NONE
    private var sessionId: Long = 0L
    private var currentSession: SessionInfo? = null

    private var pendingEnter: PendingEnter? = null

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
        resetAll()

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
                        processStateChange(result.state, result.dataSource)
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
                            val frame = mapper.map(snapshot)
                            processFrame(snapshot, frame, connectionState)
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
        SupervisorJob() + ioDispatcher + CoroutineExceptionHandler { _, e ->
            logger.error(e) { "uncaught exception" }
        },
    )

    private fun processStateChange(newState: GameConnectionState, source: DataSourceType) {
        val oldState = lastConnectionState
        if (oldState == newState) return

        lastConnectionState = newState
        logger.atInfo(RATE_LIMITED) {
            message = "ConnectionState: $oldState -> $newState (source=$source)"
        }

        if (oldState == GameConnectionState.DISCONNECTED &&
            (newState == GameConnectionState.IN_MENU || newState == GameConnectionState.IN_SESSION)
        ) {
            _events.tryEmit(TelemetryLifecycleEvent.SimConnected)
        }

        when (oldState to newState) {
            GameConnectionState.IN_SESSION to GameConnectionState.IN_MENU -> {
                if (sessionState == SessionState.RUNNING && sessionId > 0L) {
                    sessionState = SessionState.PAUSED
                    logger.atInfo(RATE_LIMITED) {
                        message = "SessionPaused id=$sessionId reason=NOT_IN_SESSION (source=$source)"
                    }
                    _events.tryEmit(TelemetryLifecycleEvent.SessionPaused(sessionId, SessionPauseReason.NOT_IN_SESSION))
                }
            }

            GameConnectionState.IN_MENU to GameConnectionState.IN_SESSION,
            GameConnectionState.DISCONNECTED to GameConnectionState.IN_SESSION -> {
                pendingEnter = PendingEnter(wasPaused = (sessionState == SessionState.PAUSED))
                logger.atDebug(RATE_LIMITED) {
                    message = "pendingEnter set (wasPaused=${pendingEnter?.wasPaused}) (source=$source)"
                }
            }

            GameConnectionState.IN_MENU to GameConnectionState.DISCONNECTED,
            GameConnectionState.IN_SESSION to GameConnectionState.DISCONNECTED -> {
                if (sessionState != SessionState.NONE && sessionId > 0L) {
                    logger.atInfo(RATE_LIMITED) {
                        message = "SessionEnded id=$sessionId reason=SIM_DISCONNECTED (source=$source)"
                    }
                    _events.tryEmit(TelemetryLifecycleEvent.SessionEnded(sessionId, SessionEndReason.SIM_DISCONNECTED))
                }
                _events.tryEmit(TelemetryLifecycleEvent.SimDisconnected)
                resetAll()
            }

            else -> Unit
        }
    }

    private suspend fun processFrame(snapshot: AcRawSnapshot, frame: TelemetryFrame, state: GameConnectionState) {
        if (state != GameConnectionState.IN_SESSION) return

        val pending = pendingEnter
        if (pending != null) {
            pendingEnter = null
            enterSessionOnFirstFrame(frame, pending.wasPaused)
        } else if (sessionState == SessionState.NONE) {
            startNewSessionFromFrame(frame, replacedOld = false)
        }

        logInferredBoundaries(frame)

        logger.atDebug(RATE_LIMITED) {
            message = "sample " + frameKeySummary(frame)
        }

        updateSessionFromFrame(frame)

        val newValidity = frame.lap?.validity ?: LapValidity.UNKNOWN
        val newLapIndex = frame.lap?.currentLapIndex

        processLapIndex(frame, newLapIndex)
        lastLapValidity = newValidity

        emitSampleIfNeeded(snapshot, frame)
    }

    private fun enterSessionOnFirstFrame(frame: TelemetryFrame, wasPaused: Boolean) {
        val isResume = wasPaused && isLikelyResume(frame)

        logger.atDebug(RATE_LIMITED) {
            message = "enterSessionOnFirstFrame wasPaused=$wasPaused isResume=$isResume " +
                "(source=$lastDataSource) " + frameKeySummary(frame)
        }

        if (isResume) {
            sessionState = SessionState.RUNNING
            if (sessionId > 0L) {
                logger.atInfo(RATE_LIMITED) { message = "SessionResumed id=$sessionId (source=$lastDataSource)" }
                _events.tryEmit(TelemetryLifecycleEvent.SessionResumed(sessionId))
            } else {
                startNewSessionFromFrame(frame, replacedOld = false)
            }
            return
        }

        startNewSessionFromFrame(frame, replacedOld = (sessionState != SessionState.NONE && sessionId > 0L))
    }

    private fun isLikelyResume(frame: TelemetryFrame): Boolean {
        val cur = currentSession ?: return false

        val car = frame.session?.car?.carModel.orEmpty().trim()
        val track = frame.session?.track?.trackId.orEmpty().trim()

        val carChanged = car.isNotBlank() && cur.carModel.isNotBlank() && car != cur.carModel
        val trackChanged = track.isNotBlank() && cur.trackId.isNotBlank() && track != cur.trackId

        if (carChanged || trackChanged) {
            logger.atDebug(RATE_LIMITED) {
                message = "resume rejected: carChanged=$carChanged trackChanged=$trackChanged " +
                    "cur(car=${cur.carModel}, track=${cur.trackId}) " +
                    "new(car=$car, track=$track)"
            }
        }

        return !(carChanged || trackChanged)
    }

    private fun startNewSessionFromFrame(frame: TelemetryFrame, replacedOld: Boolean) {
        if (replacedOld) {
            logger.atInfo(RATE_LIMITED) {
                message = "SessionEnded id=$sessionId reason=REPLACED_BY_NEW_SESSION (source=$lastDataSource)"
            }
            _events.tryEmit(TelemetryLifecycleEvent.SessionEnded(sessionId, SessionEndReason.REPLACED_BY_NEW_SESSION))
        }

        sessionId += 1L
        sessionState = SessionState.RUNNING
        resetSessionRuntime()

        val sessionType = frame.session?.sessionType ?: SessionType.UNKNOWN
        val car = frame.session?.car?.carModel.orEmpty().trim()
        val track = frame.session?.track?.trackId.orEmpty().trim()

        currentSession = SessionInfo(
            sessionId = sessionId,
            sessionType = sessionType,
            carModel = car,
            trackId = track,
        )

        logger.atInfo(RATE_LIMITED) {
            message = "SessionStarted id=$sessionId type=$sessionType " +
                "idx=${frame.session?.sessionIndex} online=${frame.session?.isOnline} " +
                "track=$track car=$car (source=$lastDataSource)"
        }

        _events.tryEmit(TelemetryLifecycleEvent.SessionStarted(currentSession!!))
    }

    private fun updateSessionFromFrame(frame: TelemetryFrame) {
        val cur = currentSession ?: return
        var updated = cur
        val changed = linkedSetOf<SessionField>()

        val newType = frame.session?.sessionType ?: SessionType.UNKNOWN
        if (newType != SessionType.UNKNOWN && newType != cur.sessionType) {
            logger.atInfo(RATE_LIMITED) {
                message = "SessionType changed: ${cur.sessionType} -> $newType (source=$lastDataSource)"
            }
            updated = updated.copy(sessionType = newType)
            changed += SessionField.SESSION_TYPE
        }

        val newCar = frame.session?.car?.carModel.orEmpty().trim()
        if (newCar.isNotBlank() && newCar != cur.carModel) {
            logger.atInfo(RATE_LIMITED) {
                message = "CarModel changed: ${cur.carModel} -> $newCar (source=$lastDataSource)"
            }
            updated = updated.copy(carModel = newCar)
            changed += SessionField.CAR_MODEL
        }

        val newTrack = frame.session?.track?.trackId.orEmpty().trim()
        if (newTrack.isNotBlank() && newTrack != cur.trackId) {
            logger.atInfo(RATE_LIMITED) {
                message = "TrackId changed: ${cur.trackId} -> $newTrack (source=$lastDataSource)"
            }
            updated = updated.copy(trackId = newTrack)
            changed += SessionField.TRACK_ID
        }

        if (changed.isNotEmpty()) {
            currentSession = updated
            _events.tryEmit(TelemetryLifecycleEvent.SessionUpdated(updated, changed.toSet()))
        }
    }

    private fun processLapIndex(frame: TelemetryFrame, newLapIndex: Int?) {
        val prevLap = lastLapIndex

        when {
            newLapIndex != null && prevLap == null -> {
                logger.atInfo(RATE_LIMITED) {
                    message = "LapStarted lap=$newLapIndex (source=$lastDataSource)"
                }
                _events.tryEmit(LapStarted(newLapIndex))
            }

            newLapIndex != null && prevLap != null && newLapIndex != prevLap -> {
                val lastLapTime = frame.lap?.lastLapTimeMs
                logger.atInfo(RATE_LIMITED) {
                    message = "LapFinished lap=$prevLap validity=$lastLapValidity " +
                        "lastLapTimeMs=$lastLapTime (source=$lastDataSource)"
                }
                _events.tryEmit(LapFinished(prevLap, lastLapValidity))

                logger.atInfo(RATE_LIMITED) {
                    message = "LapStarted lap=$newLapIndex (source=$lastDataSource)"
                }
                _events.tryEmit(LapStarted(newLapIndex))
            }
        }

        lastLapIndex = newLapIndex
    }

    private fun logInferredBoundaries(frame: TelemetryFrame) {
        val s = frame.session ?: return

        val idx = s.sessionIndex
        if (idx!! >= 0 && lastSessionIndex != null && idx != lastSessionIndex) {
            logger.atInfo(RATE_LIMITED) {
                message = "[boundary] sessionIndex: $lastSessionIndex -> $idx " +
                    "type=${s.sessionType} track=${s.track?.trackId} car=${s.car?.carModel} (source=$lastDataSource)"
            }
        }
        idx.let { if (it >= 0) lastSessionIndex = idx }

        val laps = s.completedLaps
        lastCompletedLaps = laps

        val left = s.sessionTimeLeftSec
        val prevLeft = lastSessionTimeLeftSec
        if (prevLeft != null) {
            val jumpUp = left?.let { (it - prevLeft) > 120f }
            if (jumpUp == true) {
                logger.atInfo(RATE_LIMITED) {
                    message = "[boundary] sessionTimeLeft jump up: $prevLeft -> $left " +
                        "idx=${s.sessionIndex} type=${s.sessionType} (source=$lastDataSource)"
                }
            }
        }
        lastSessionTimeLeftSec = left
    }

    private fun frameKeySummary(frame: TelemetryFrame): String {
        val s = frame.session
        val track = s?.track?.trackId
        val car = s?.car?.carModel
        val idx = s?.sessionIndex
        val type = s?.sessionType
        val laps = s?.completedLaps
        val left = s?.sessionTimeLeftSec
        val lap = frame.lap?.currentLapIndex
        return "track=$track car=$car type=$type idx=$idx laps=$laps left=$left lap=$lap"
    }

    private suspend fun emitSampleIfNeeded(snapshot: AcRawSnapshot, frame: TelemetryFrame) {
        if (sessionState != SessionState.RUNNING || sessionId <= 0L) return
        recordingEmitter.emitSample(sessionId, snapshot, frame, lastDataSource)
    }

    private fun resetSessionRuntime() {
        lastLapIndex = null
        lastLapValidity = LapValidity.UNKNOWN
    }

    private fun resetAll() {
        resetSessionRuntime()
        lastConnectionState = GameConnectionState.DISCONNECTED
        lastDataSource = DataSourceType.NATIVE
        lastSessionIndex = null
        lastCompletedLaps = null
        lastSessionTimeLeftSec = null

        sessionState = SessionState.NONE
        pendingEnter = null
        currentSession = null
    }
}
