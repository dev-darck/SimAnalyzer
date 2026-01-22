package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.PollResult
import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcMapper
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.contract.LapValidity
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

    private val _events = MutableSharedFlow<TelemetryLifecycleEvent>(replay = 1)
    override val events = _events.asSharedFlow()

    private var lastLapIndex: Int? = null
    private var lastSessionType: SessionType = SessionType.UNKNOWN
    private var lastLapValidity: LapValidity = LapValidity.UNKNOWN
    private var lastConnectionState: GameConnectionState = GameConnectionState.DISCONNECTED

    private val _frames: MutableSharedFlow<TelemetryFrame> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val frames: SharedFlow<TelemetryFrame> = _frames.asSharedFlow()

    override suspend fun launchTelemetry() {
        launchLoop()
    }

    override suspend fun finishTelemetry() {
        appScope.cancel()

        withContext(ioDispatcher) {
            shm.close()
            pollLoop.stop()
        }

        resetState()
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

    private fun resetState() {
        lastLapIndex = null
        lastSessionType = SessionType.UNKNOWN
        lastLapValidity = LapValidity.UNKNOWN
        lastConnectionState = GameConnectionState.DISCONNECTED
    }

    private fun processStateChange(newState: GameConnectionState) {
        val oldState = lastConnectionState
        if (oldState == newState) return

        lastConnectionState = newState
        logger.info { "ConnectionState: $oldState -> $newState" }

        when (oldState to newState) {
            GameConnectionState.DISCONNECTED to GameConnectionState.IN_MENU,
            GameConnectionState.DISCONNECTED to GameConnectionState.IN_SESSION -> {
                _events.tryEmit(TelemetryLifecycleEvent.SimConnected)
            }

            GameConnectionState.IN_SESSION to GameConnectionState.IN_MENU -> {
                _events.tryEmit(TelemetryLifecycleEvent.SessionEnded)
                resetSessionState()
            }

            GameConnectionState.IN_MENU to GameConnectionState.DISCONNECTED -> {
                _events.tryEmit(TelemetryLifecycleEvent.SimDisconnected)
                resetSessionState()
            }

            GameConnectionState.IN_SESSION to GameConnectionState.DISCONNECTED -> {
                _events.tryEmit(TelemetryLifecycleEvent.SessionEnded)
                _events.tryEmit(TelemetryLifecycleEvent.SimDisconnected)
                resetSessionState()
            }
        }
    }

    private fun resetSessionState() {
        lastLapIndex = null
        lastSessionType = SessionType.UNKNOWN
        lastLapValidity = LapValidity.UNKNOWN
    }

    private fun processFrame(frame: TelemetryFrame, state: GameConnectionState) {
        if (state != GameConnectionState.IN_SESSION) return

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
