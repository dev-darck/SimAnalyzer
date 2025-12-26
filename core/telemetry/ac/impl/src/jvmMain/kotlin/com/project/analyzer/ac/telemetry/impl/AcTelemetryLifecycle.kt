package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.api.di.AppCoroutine
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.TelemetryDataSource
import com.project.analyzer.telemetry.ac.api.contract.LapValidity
import com.project.analyzer.telemetry.ac.api.contract.SessionPhase
import com.project.analyzer.telemetry.ac.api.contract.SessionType
import com.project.analyzer.telemetry.ac.api.contract.SimStatus
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent.LapFinished
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent.LapStarted
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
class AcTelemetryLifecycle(
    private val dataSource: TelemetryDataSource,
    @param:AppCoroutine
    private val appScope: CoroutineScope,
) : TelemetryLifecycle {

    private val _simStatus = MutableStateFlow(SimStatus.OFF)
    override val simStatus = _simStatus.asStateFlow()

    private val _sessionType = MutableStateFlow(SessionType.UNKNOWN)
    override val sessionType = _sessionType.asStateFlow()

    private val _sessionPhase = MutableStateFlow(SessionPhase.NONE)
    override val sessionPhase = _sessionPhase.asStateFlow()

    private val _lapValidity = MutableStateFlow(LapValidity.UNKNOWN)
    override val lapValidity = _lapValidity.asStateFlow()

    private val _events = MutableSharedFlow<TelemetryLifecycleEvent>(
        extraBufferCapacity = 64
    )

    private var lastLapIndex: Int? = null
    private var lastSim: SimStatus = SimStatus.OFF
    private var lastSessionType: SessionType = SessionType.UNKNOWN
    private var lastPhase: SessionPhase = SessionPhase.NONE

    override val events = _events.asSharedFlow()

    private var job: Job? = null

    override fun start() {
        if (job != null) return

        job = appScope.launch {
            dataSource.frames().collectLatest { frame ->
                handleFrame(frame)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private fun handleFrame(frame: TelemetryFrame) {
        val newSim = mapSimStatus(frame)
        val newType = mapSessionType(frame)
        val newPhase = mapSessionPhase(frame)
        val newValidity = mapLapValidity(frame)
        val newLapIndex = mapLapIndex(frame)

        handleSimStatus(newSim)

        handleNewSessionType(newSim, newType)

        handleNewPhase(newPhase)

        if (newValidity != _lapValidity.value) {
            _lapValidity.value = newValidity
        }

        handleNewLapIndex(newLapIndex)
    }

    private fun handleNewLapIndex(newLapIndex: Int?) {
        val prevLap = lastLapIndex
        if (newLapIndex != null) {
            if (prevLap == null) {
                _events.tryEmit(LapStarted(newLapIndex))
            } else if (newLapIndex != prevLap) {
                _events.tryEmit(LapFinished(prevLap, _lapValidity.value))
                _events.tryEmit(LapStarted(newLapIndex))
            }
        }
        lastLapIndex = newLapIndex
    }

    private fun handleNewPhase(newPhase: SessionPhase) {
        if (newPhase != lastPhase) {
            _sessionPhase.value = newPhase
            if (newPhase == SessionPhase.NONE && lastPhase != SessionPhase.NONE) {
                _events.tryEmit(TelemetryLifecycleEvent.SessionEnded)
            }
            lastPhase = newPhase
        }
    }

    private fun handleNewSessionType(newSim: SimStatus, newType: SessionType) {
        if (newType != lastSessionType) {
            _sessionType.value = newType
            if (newSim == SimStatus.LIVE && newType != SessionType.UNKNOWN) {
                _events.tryEmit(TelemetryLifecycleEvent.SessionStarted(newType))
            }
            lastSessionType = newType
        }
    }

    private fun handleSimStatus(newSim: SimStatus) {
        if (newSim != lastSim) {
            _simStatus.value = newSim
            if (lastSim != SimStatus.LIVE && newSim == SimStatus.LIVE) {
                _events.tryEmit(TelemetryLifecycleEvent.SimConnected)
            }
            if (lastSim == SimStatus.LIVE && newSim != SimStatus.LIVE) {
                _events.tryEmit(TelemetryLifecycleEvent.SimDisconnected)
                _events.tryEmit(TelemetryLifecycleEvent.SessionEnded)
            }
            lastSim = newSim
        }
    }

    private fun mapSimStatus(frame: TelemetryFrame): SimStatus {
        return frame.session?.status ?: SimStatus.OFF
    }

    private fun mapSessionType(frame: TelemetryFrame): SessionType {
        return frame.session?.sessionType ?: SessionType.UNKNOWN
    }

    private fun mapSessionPhase(frame: TelemetryFrame): SessionPhase {
        return frame.session?.phase ?: SessionPhase.NONE
    }

    private fun mapLapValidity(frame: TelemetryFrame): LapValidity {
        return frame.lap?.validity ?: LapValidity.UNKNOWN
    }

    private fun mapLapIndex(frame: TelemetryFrame): Int? {
        return frame.lap?.currentLapIndex
    }
}
