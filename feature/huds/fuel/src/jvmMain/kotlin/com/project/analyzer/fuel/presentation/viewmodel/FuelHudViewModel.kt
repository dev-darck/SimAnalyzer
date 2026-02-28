package com.project.analyzer.fuel.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.project.analyzer.fuel.domain.model.FuelEstimate
import com.project.analyzer.fuel.domain.model.FuelIdentityKey
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.fuel.domain.model.FuelResult
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionConfig
import com.project.analyzer.fuel.domain.usecase.FuelConsumptionUseCase
import com.project.analyzer.fuel.presentation.FuelHudUiState
import com.project.analyzer.fuel.presentation.map.toUiState
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Inject
internal class FuelHudViewModel(private val useCase: FuelConsumptionUseCase) :
    LeakAwareMviViewModel<FuelIntent, FuelHudUiState>(FuelHudUiState()) {

    private val safetyFactor: Double get() = 1.0 + (FuelConsumptionConfig.safetyMarginPercent / 100.0)

    private var currentKey: FuelIdentityKey = FuelIdentityKey.UNKNOWN
    private val peaksByKey: MutableMap<FuelIdentityKey, Session> = mutableMapOf()
    private var fuelEstimateJob: Job? = null
    private var lastDataUiEmitNs: Long = 0L

    override suspend fun handleIntent(intent: FuelIntent) {
        when (intent) {
            FuelIntent.Start -> subscribeToEstimates()

            FuelIntent.ResetAll -> {
                peaksByKey[currentKey]?.reset()
                useCase.resetAll()
            }
        }
    }

    private fun subscribeToEstimates() {
        fuelEstimateJob?.cancel()
        fuelEstimateJob = useCase.fuelEstimates
            .conflate()
            .onEach(::handleFuelResult)
            .launchIn(viewModelScope)
    }

    private fun handleFuelResult(result: FuelResult) {
        when (result) {
            FuelResult.SessionEnded -> {
                lastDataUiEmitNs = 0L
                updateState { copy(isShow = false, isSessionActive = false) }
            }

            FuelResult.SessionPaused -> {
                lastDataUiEmitNs = 0L
                updateState { copy(isShow = false, isSessionActive = false) }
            }

            FuelResult.Reset -> {
                peaksByKey[currentKey]?.reset()
                lastDataUiEmitNs = 0L
                updateState { copy(isShow = true, isSessionActive = true) }
            }

            FuelResult.NoData -> updateState { copy(isShow = true, isSessionActive = true) }

            is FuelResult.Data -> handleDataResult(result)
        }
    }

    private fun handleDataResult(result: FuelResult.Data) {
        val estimate = result.estimate
        val newKey = FuelIdentityKey.from(estimate.carId, estimate.trackId) ?: FuelIdentityKey.UNKNOWN
        if (newKey != currentKey) {
            currentKey = newKey
            lastDataUiEmitNs = 0L
        }

        updatePeakStateFromEstimate(estimate)

        val nowNs = System.nanoTime()
        val current = currentState
        val forceEmit = shouldForceDataEmit(current, estimate)
        if (!forceEmit && nowNs - lastDataUiEmitNs < DATA_UI_EMIT_INTERVAL_NS) return

        val baseUiState = estimate.toUiState(safetyFactor)
        val withPeaks = applyPeakProjection(baseUiState)

        lastDataUiEmitNs = nowNs
        setState(withPeaks.copy(isShow = true, isSessionActive = true))
    }

    private fun updatePeakStateFromEstimate(estimate: FuelEstimate) {
        val peakState = peaksByKey.getOrPut(currentKey) { Session() }
        val canUpdatePeak = estimate.phase == FuelPhase.PER_LAP && estimate.isCurrentLapValid

        if (canUpdatePeak) {
            peakState.updatePeakLitersPerLap(estimate.litersPerLap)
            val lpl = estimate.litersPerLap
            FuelConsumptionConfig.planLaps.forEachIndexed { index, laps ->
                peakState.updatePeakPlanFuel(index, lpl?.times(laps)?.times(safetyFactor))
            }
        }
    }

    private fun applyPeakProjection(fresh: FuelHudUiState): FuelHudUiState {
        val peakState = peaksByKey.getOrPut(currentKey) { Session() }
        return fresh.copy(
            peakValue = peakState.peakLitersPerLap?.let { "%.2f L".format(Locale.US, it) } ?: "—",
            peakLitersPerLapRaw = peakState.peakLitersPerLap,
            planRows = fresh.planRows.mapIndexed { index, row ->
                val peakLiters = peakState.peakPlanFuelLiters.getOrNull(index)
                row.copy(peakFuelText = peakLiters?.let { "${it.roundToInt()} L" } ?: "—")
            },
        )
    }

    private fun shouldForceDataEmit(current: FuelHudUiState, estimate: FuelEstimate): Boolean {
        if (!current.isShow || !current.isSessionActive) return true
        if (current.phase != estimate.phase) return true
        if (current.isCurrentLapValid != estimate.isCurrentLapValid) return true
        return false
    }

    private companion object {
        val DATA_UI_EMIT_INTERVAL_NS: Long = 50.milliseconds.inWholeNanoseconds // 20 Hz
    }
}
