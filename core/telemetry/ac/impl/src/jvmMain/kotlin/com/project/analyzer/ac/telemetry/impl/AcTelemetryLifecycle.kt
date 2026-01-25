package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.PollResult
import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcMapper
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.contract.LapValidity
import com.project.analyzer.telemetry.ac.api.contract.SessionEndReason
import com.project.analyzer.telemetry.ac.api.contract.SessionField
import com.project.analyzer.telemetry.ac.api.contract.SessionInfo
import com.project.analyzer.telemetry.ac.api.contract.SessionPauseReason
import com.project.analyzer.telemetry.ac.api.contract.SessionType
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent.LapFinished
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent.LapStarted
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
class AcTelemetryLifecycle(
    private val pollLoop: AcPollLoop,
    private val mapper: AcMapper,
    private val shm: AcSharedMemory,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryLifecycle {

    private val appScope = CoroutineScope(SupervisorJob() + ioDispatcher + CoroutineExceptionHandler { _, _ -> })

    private val _events = MutableSharedFlow<TelemetryLifecycleEvent>(
        replay = 1,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events = _events.asSharedFlow()

    private val _frames: MutableSharedFlow<TelemetryFrame> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val frames: SharedFlow<TelemetryFrame> = _frames.asSharedFlow()

    private var lastLapIndex: Int? = null
    private var lastLapValidity: LapValidity = LapValidity.UNKNOWN
    private var lastConnectionState: GameConnectionState = GameConnectionState.DISCONNECTED

    private var sessionState: SessionState = SessionState.NONE
    private var sessionId: Long = 0L
    private var currentSession: SessionInfo? = null

    private var pendingEnter: PendingEnter? = null

    override suspend fun launchTelemetry() {
        launchLoop()
    }

    override suspend fun finishTelemetry() {
        appScope.cancel()

        withContext(ioDispatcher) {
            shm.close()
            pollLoop.stop()
        }

        resetAll()
    }

    private fun launchLoop() {
        appScope.launch {
            var connectionState: GameConnectionState = GameConnectionState.DISCONNECTED

            pollLoop.start { result ->
                when (result) {
                    is PollResult.StateChanged -> {
                        connectionState = result.state
                        processStateChange(result.state)
                    }

                    is PollResult.Frame -> {
                        val frame = mapper.map(result.snapshot)

                        processFrame(frame, connectionState)

                        if (connectionState == GameConnectionState.IN_SESSION) {
                            _frames.emit(frame)
                        }
                    }
                }
            }
        }
    }

    private fun processStateChange(newState: GameConnectionState) {
        val oldState = lastConnectionState
        if (oldState == newState) return

        lastConnectionState = newState
        logger.info { "ConnectionState: $oldState -> $newState" }

        if (oldState == GameConnectionState.DISCONNECTED &&
            (newState == GameConnectionState.IN_MENU || newState == GameConnectionState.IN_SESSION)
        ) {
            _events.tryEmit(TelemetryLifecycleEvent.SimConnected)
        }

        when (oldState to newState) {
            GameConnectionState.IN_SESSION to GameConnectionState.IN_MENU -> {
                if (sessionState == SessionState.RUNNING && sessionId > 0L) {
                    sessionState = SessionState.PAUSED
                    _events.tryEmit(
                        TelemetryLifecycleEvent.SessionPaused(
                            sessionId = sessionId,
                            reason = SessionPauseReason.NOT_IN_SESSION
                        )
                    )
                }
            }

            GameConnectionState.IN_MENU to GameConnectionState.IN_SESSION,
            GameConnectionState.DISCONNECTED to GameConnectionState.IN_SESSION -> {
                pendingEnter = PendingEnter(wasPaused = (sessionState == SessionState.PAUSED))
            }

            GameConnectionState.IN_MENU to GameConnectionState.DISCONNECTED,
            GameConnectionState.IN_SESSION to GameConnectionState.DISCONNECTED -> {
                if (sessionState != SessionState.NONE && sessionId > 0L) {
                    _events.tryEmit(
                        TelemetryLifecycleEvent.SessionEnded(
                            sessionId = sessionId,
                            reason = SessionEndReason.SIM_DISCONNECTED
                        )
                    )
                }
                _events.tryEmit(TelemetryLifecycleEvent.SimDisconnected)
                resetAll()
            }

            else -> Unit
        }
    }

    private fun processFrame(frame: TelemetryFrame, state: GameConnectionState) {
        if (state != GameConnectionState.IN_SESSION) return

        val pending = pendingEnter
        if (pending != null) {
            pendingEnter = null
            enterSessionOnFirstFrame(frame, pending.wasPaused)
        } else if (sessionState == SessionState.NONE) {
            startNewSessionFromFrame(frame, replacedOld = false)
        }

        updateSessionFromFrame(frame)

        val newValidity = frame.lap?.validity ?: LapValidity.UNKNOWN
        val newLapIndex = frame.lap?.currentLapIndex

        processLapIndex(newLapIndex)
        lastLapValidity = newValidity
    }

    private fun enterSessionOnFirstFrame(frame: TelemetryFrame, wasPaused: Boolean) {
        val isResume = wasPaused && isLikelyResume(frame)

        if (isResume) {
            sessionState = SessionState.RUNNING
            if (sessionId > 0L) {
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

        return !(carChanged || trackChanged)
    }

    private fun startNewSessionFromFrame(frame: TelemetryFrame, replacedOld: Boolean) {
        if (replacedOld) {
            _events.tryEmit(
                TelemetryLifecycleEvent.SessionEnded(
                    sessionId = sessionId,
                    reason = SessionEndReason.REPLACED_BY_NEW_SESSION
                )
            )
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
            trackId = track
        )

        _events.tryEmit(TelemetryLifecycleEvent.SessionStarted(currentSession!!))
    }

    private fun updateSessionFromFrame(frame: TelemetryFrame) {
        val cur = currentSession ?: return
        var updated = cur
        val changed = linkedSetOf<SessionField>()

        val newType = frame.session?.sessionType ?: SessionType.UNKNOWN
        if (newType != SessionType.UNKNOWN && newType != cur.sessionType) {
            updated = updated.copy(sessionType = newType)
            changed += SessionField.SESSION_TYPE
        }

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
            _events.tryEmit(TelemetryLifecycleEvent.SessionUpdated(updated, changed.toSet()))
        }
    }

    private fun processLapIndex(newLapIndex: Int?) {
        val prevLap = lastLapIndex

        when {
            newLapIndex != null && prevLap == null -> {
                _events.tryEmit(LapStarted(newLapIndex))
            }

            newLapIndex != null && prevLap != null && newLapIndex != prevLap -> {
                _events.tryEmit(LapFinished(prevLap, lastLapValidity))
                _events.tryEmit(LapStarted(newLapIndex))
            }
        }

        lastLapIndex = newLapIndex
    }

    private fun resetSessionRuntime() {
        lastLapIndex = null
        lastLapValidity = LapValidity.UNKNOWN
    }

    private fun resetAll() {
        resetSessionRuntime()
        lastConnectionState = GameConnectionState.DISCONNECTED

        sessionState = SessionState.NONE
        pendingEnter = null
        currentSession = null

    }

    private enum class SessionState { NONE,
        RUNNING,
        PAUSED
    }

    private data class PendingEnter(val wasPaused: Boolean)
}
