package com.project.analyzer.fuel.domain.usecase

import com.project.analyzer.fuel.data.model.SavedFuelData
import com.project.analyzer.fuel.domain.model.FuelIdentityKey
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.fuel.domain.model.FuelResult
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionEngine
import com.project.analyzer.fuel.domain.repository.FuelRepository
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

@Inject
@SingleIn(HudScope::class)
internal class FuelConsumptionUseCaseImpl(
    private val telemetry: TelemetryLifecycle,
    private val engine: FuelConsumptionEngine,
    private val repository: FuelRepository,
) : FuelConsumptionUseCase {

    private var mode: Mode = Mode.NONE
    private var activeSessionId: Long = 0L

    private var currentSession: SessionInfo? = null
    private var savedFuelData: SavedFuelData? = null

    private var sessionPeakLitersPerLap: Double = 0.0
    private var sessionBestValidLapTimeMs: Int? = null

    private var lastIdentityLog: String = ""

    private val manualResetFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val fuelEstimates: Flow<FuelResult>
        get() = channelFlow {

            val inputs = merge(
                telemetry.frames.map { Input.Frame(it) },
                telemetry.events.map { Input.Event(it) },
                manualResetFlow.map { Input.ManualReset },
            )

            inputs.collect { input ->
                val out: FuelResult? = when (input) {
                    is Input.Frame -> onFrame(input.frame)

                    is Input.Event -> onLifecycleEvent(input.event)

                    Input.ManualReset -> {
                        onManualReset()
                        FuelResult.Reset
                    }
                }

                if (out != null) send(out)
            }
        }

    private suspend fun onLifecycleEvent(event: TelemetryLifecycleEvent): FuelResult? {
        return when (event) {
            is TelemetryLifecycleEvent.SimDisconnected -> {
                flushIfNeeded(reason = "simDisconnected")
                resetState(full = true)
                FuelResult.SessionEnded
            }

            is TelemetryLifecycleEvent.SessionStarted -> {
                if (activeSessionId != 0L && activeSessionId != event.session.sessionId) {
                    flushIfNeeded(reason = "sessionReplacedByStart")
                }

                activeSessionId = event.session.sessionId
                mode = Mode.RUNNING

                currentSession = event.session
                sessionPeakLitersPerLap = 0.0
                sessionBestValidLapTimeMs = null
                engine.reset()

                savedFuelData = loadIfIdentityReady(event.session)

                logIdentity("SessionStarted", event.session)

                FuelResult.Reset
            }

            is TelemetryLifecycleEvent.SessionUpdated -> {
                if (event.session.sessionId != activeSessionId) return null

                val prev = currentSession
                currentSession = mergeSticky(prev, event.session)

                val merged = currentSession ?: return null
                val identityChanged = didIdentityChange(prev, merged)

                val prevKey = prev?.toFuelIdentityKey()
                val newKey = merged.toFuelIdentityKey()
                val becameReady = prevKey == null && newKey != null

                if (becameReady) {
                    savedFuelData = loadIfIdentityReady(merged)
                    logIdentity("SessionUpdated(identityReady)", merged)
                    return null
                }

                if (identityChanged) {
                    flushIfNeeded(reason = "identityChangedInSession")
                    sessionPeakLitersPerLap = 0.0
                    sessionBestValidLapTimeMs = null
                    engine.reset()
                    savedFuelData = loadIfIdentityReady(merged)

                    logIdentity("SessionUpdated(identityChanged)", merged)
                    return FuelResult.Reset
                }

                null
            }

            is TelemetryLifecycleEvent.SessionPaused -> {
                if (event.sessionId != activeSessionId) return null
                if (mode == Mode.RUNNING) {
                    mode = Mode.PAUSED
                    flushIfNeeded(reason = "sessionPaused")

                    return FuelResult.SessionPaused
                }
                null
            }

            is TelemetryLifecycleEvent.SessionResumed -> {
                if (event.sessionId != activeSessionId) return null

                val wasPaused = (mode == Mode.PAUSED)
                mode = Mode.RUNNING

                if (wasPaused) FuelResult.NoData else null
            }

            is TelemetryLifecycleEvent.SessionEnded -> {
                if (event.sessionId != activeSessionId) return null

                flushIfNeeded(reason = "sessionEnded:${event.reason}")
                resetState(full = true)

                FuelResult.SessionEnded
            }

            else -> null
        }
    }

    private fun onFrame(frame: TelemetryFrame): FuelResult {
        if (mode != Mode.RUNNING || activeSessionId == 0L) return FuelResult.NoData

        val estimate = engine.onFrame(frame, savedFuelData) ?: return FuelResult.NoData

        if (estimate.phase == FuelPhase.PER_LAP && estimate.isCurrentLapValid) {
            val lpl = estimate.litersPerLap
            if (lpl != null && lpl.isFinite() && lpl > sessionPeakLitersPerLap) {
                sessionPeakLitersPerLap = lpl
            }
        }

        frame.lap?.bestLapTimeMs?.let { bestMs ->
            if (bestMs > 0) {
                val cur = sessionBestValidLapTimeMs
                if (cur == null || bestMs < cur) sessionBestValidLapTimeMs = bestMs
            }
        }

        return FuelResult.Data(estimate)
    }

    override suspend fun resetAll() {
        manualResetFlow.emit(Unit)
    }

    private suspend fun onManualReset() {
        engine.reset()
        sessionPeakLitersPerLap = 0.0
        sessionBestValidLapTimeMs = null
        savedFuelData = null

        currentSession?.toFuelIdentityKey()?.let { key ->
            repository.clear(
                carModel = key.carModel,
                trackId = key.trackId,
            )
        }
    }

    private suspend fun flushIfNeeded(reason: String) {
        val key = currentSession?.toFuelIdentityKey() ?: return

        val peak = sessionPeakLitersPerLap
        val best = sessionBestValidLapTimeMs
        if (peak <= 0.0 && best == null) return

        repository.updateIfBetter(
            carModel = key.carModel,
            trackId = key.trackId,
            peakLitersPerLap = peak.takeIf { it > 0.0 },
            bestValidLapTimeMs = best,
        )
        logger.info { "Fuel flush ($reason) key=${key.composite} peak=$peak bestMs=$best" }
    }

    private suspend fun loadIfIdentityReady(session: SessionInfo): SavedFuelData? {
        val key = session.toFuelIdentityKey() ?: return null
        return repository.load(
            carModel = key.carModel,
            trackId = key.trackId,
        )
    }

    private fun resetState(full: Boolean) {
        mode = Mode.NONE
        engine.reset()
        sessionPeakLitersPerLap = 0.0
        sessionBestValidLapTimeMs = null
        savedFuelData = null

        if (full) {
            activeSessionId = 0L
            currentSession = null
        }
    }

    private fun mergeSticky(old: SessionInfo?, incoming: SessionInfo): SessionInfo {
        if (old == null) return incoming

        fun pick(oldVal: String, newVal: String): String = newVal.ifBlank { oldVal }

        return old.copy(
            sessionId = incoming.sessionId,
            sessionType = if (incoming.sessionType != old.sessionType && incoming.sessionType.name != "UNKNOWN") {
                incoming.sessionType
            } else {
                old.sessionType
            },
            carModel = pick(old.carModel, incoming.carModel),
            trackId = pick(old.trackId, incoming.trackId),
        )
    }

    private fun didIdentityChange(old: SessionInfo?, new: SessionInfo): Boolean {
        if (old == null) return false
        val oldKey = old.toFuelIdentityKey() ?: return false
        val newKey = new.toFuelIdentityKey() ?: return false
        return oldKey != newKey
    }

    private fun logIdentity(prefix: String, session: SessionInfo) {
        val key = session.toFuelIdentityKey()
        val msg = "$prefix sessionId=${session.sessionId} key=${key?.composite ?: "<pending>"}"
        if (msg != lastIdentityLog) {
            lastIdentityLog = msg
            logger.info { msg }
        }
    }

    private fun SessionInfo.toFuelIdentityKey(): FuelIdentityKey? = FuelIdentityKey.from(
        carModel = carModel,
        trackId = trackId,
    )

    sealed interface Input {
        data class Frame(val frame: TelemetryFrame) : Input
        data class Event(val event: TelemetryLifecycleEvent) : Input
        data object ManualReset : Input
    }

    private enum class Mode {
        NONE,
        RUNNING,
        PAUSED,
    }
}
