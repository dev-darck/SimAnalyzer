package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.PollResult
import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcMapper
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.api.di.AppCoroutine
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.contract.LapValidity
import com.project.analyzer.telemetry.ac.api.contract.SessionType
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent.LapFinished
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent.LapStarted
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
class AcTelemetryLifecycle(
    private val pollLoop: AcPollLoop,
    private val mapper: AcMapper,
    private val shm: AcSharedMemory,
    @param:AppCoroutine
    private val appScope: CoroutineScope,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryLifecycle {

    private val _events = MutableSharedFlow<TelemetryLifecycleEvent>(extraBufferCapacity = 64)
    override val events = _events.asSharedFlow()

    private var lastLapIndex: Int? = null
    private var lastSessionType: SessionType = SessionType.UNKNOWN
    private var lastLapValidity: LapValidity = LapValidity.UNKNOWN
    private var lastConnectionState: GameConnectionState = GameConnectionState.DISCONNECTED

    private var collectionJob: Job? = null

    override val frames: SharedFlow<TelemetryFrame> by lazy {
        createFramesFlow()
    }

    private fun createFramesFlow(): SharedFlow<TelemetryFrame> {
        val rawFrames = callbackFlow {
            pollLoop.start { result ->
                when (result) {
                    is PollResult.StateChanged -> {
                        processStateChange(result.state)
                    }

                    is PollResult.Frame -> {
                        trySend(mapper.map(result.snapshot))
                    }
                }
            }
            awaitClose { pollLoop.stop() }
        }
            .buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .catch { e -> println("Telemetry error: $e") }
            .flowOn(ioDispatcher)

        return rawFrames
            .onEach { frame -> processFrame(frame) }
            .shareIn(
                scope = appScope,
                started = SharingStarted.Eagerly,
                replay = 1
            )
    }

    override suspend fun finishTelemetry() {
        collectionJob?.cancel()
        collectionJob = null

        withContext(ioDispatcher) {
            shm.close()
            pollLoop.stop()
        }

        resetState()
    }

    private fun resetState() {
        lastLapIndex = null
        lastSessionType = SessionType.UNKNOWN
        lastLapValidity = LapValidity.UNKNOWN
        lastConnectionState = GameConnectionState.DISCONNECTED
    }

    private fun processStateChange(newState: GameConnectionState) {
        val oldState = lastConnectionState
        lastConnectionState = newState

        when {
            // Game just connected (was disconnected, now in menu or session)
            oldState == GameConnectionState.DISCONNECTED && newState != GameConnectionState.DISCONNECTED -> {
                _events.tryEmit(TelemetryLifecycleEvent.SimConnected)
            }

            // Game disconnected
            newState == GameConnectionState.DISCONNECTED && oldState != GameConnectionState.DISCONNECTED -> {
                if (oldState == GameConnectionState.IN_SESSION) {
                    _events.tryEmit(TelemetryLifecycleEvent.SessionEnded)
                }
                _events.tryEmit(TelemetryLifecycleEvent.SimDisconnected)
                resetSessionState()
            }

            // Went from session to menu
            oldState == GameConnectionState.IN_SESSION && newState == GameConnectionState.IN_MENU -> {
                _events.tryEmit(TelemetryLifecycleEvent.SessionEnded)
                resetSessionState()
            }
        }
    }

    private fun resetSessionState() {
        lastLapIndex = null
        lastSessionType = SessionType.UNKNOWN
        lastLapValidity = LapValidity.UNKNOWN
    }

    private fun processFrame(frame: TelemetryFrame) {
        // Only process session/lap data when in session
        if (lastConnectionState != GameConnectionState.IN_SESSION) return

        val newType = frame.session?.sessionType ?: SessionType.UNKNOWN
        val newValidity = frame.lap?.validity ?: LapValidity.UNKNOWN
        val newLapIndex = frame.lap?.currentLapIndex

        processSessionType(newType)
        processLapIndex(newLapIndex)

        lastLapValidity = newValidity
    }

    private fun processSessionType(newType: SessionType) {
        if (newType == lastSessionType) return

        if (newType != SessionType.UNKNOWN) {
            _events.tryEmit(TelemetryLifecycleEvent.SessionStarted(newType))
        } else if (lastSessionType != SessionType.UNKNOWN) {
            _events.tryEmit(TelemetryLifecycleEvent.SessionEnded)
        }
        lastSessionType = newType
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
}
