package com.project.analyzer.fuel.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.fuel.domain.model.FuelResult
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionConfig
import com.project.analyzer.fuel.domain.usecase.FuelConsumptionUseCase
import com.project.analyzer.fuel.presentation.FuelHudUiState
import com.project.analyzer.fuel.presentation.map.toUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

internal class FuelHudViewModel(
    private val useCase: FuelConsumptionUseCase,
) : ViewModel() {

    private val safetyFactor: Double get() = 1.0 + (FuelConsumptionConfig.safetyMarginPercent / 100.0)

    private var currentKey: SessionKey = SessionKey(null, null)
    private val peaksByKey: MutableMap<SessionKey, PeakState> = mutableMapOf()
    private var fuelEstimateJob: Job? = null

    private val _state = MutableStateFlow(FuelHudUiState())
    val state: StateFlow<FuelHudUiState> = _state.asStateFlow()

    fun dispatch(intent: FuelIntent) {
        when (intent) {
            is FuelIntent.Start -> subscribeToEstimates()

            is FuelIntent.ResetAll -> {
                peaksByKey[currentKey]?.reset()
                viewModelScope.launch {
                    useCase.resetAll()
                }
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
            FuelResult.SessionEnded -> updateState { copy(isShow = false, isSessionActive = false) }

            FuelResult.SessionPaused -> updateState { copy(isShow = false, isSessionActive = false) }

            FuelResult.Reset -> {
                peaksByKey[currentKey]?.reset()
                updateState { copy(isShow = true, isSessionActive = true) }
            }

            FuelResult.NoData -> updateState { copy(isShow = true, isSessionActive = true) }
            is FuelResult.Data -> handleDataResult(result)
        }
    }

    private fun handleDataResult(result: FuelResult.Data) {
        val estimate = result.estimate
        val newKey = SessionKey(estimate.carModel, estimate.trackId)
        if (newKey != currentKey) {
            currentKey = newKey
        }

        val baseUiState = estimate.toUiState(safetyFactor)
        val withPeaks = applyPeakTracking(baseUiState)

        updateState { withPeaks.copy(isShow = true, isSessionActive = true) }
    }

    private fun applyPeakTracking(fresh: FuelHudUiState): FuelHudUiState {
        val peakState = peaksByKey.getOrPut(currentKey) { PeakState() }
        val canUpdatePeak = fresh.phase == FuelPhase.PER_LAP && fresh.isCurrentLapValid

        if (canUpdatePeak) {
            peakState.updatePeakLitersPerLap(fresh.litersPerLapRaw)
            fresh.planFuelLitersRaw.forEachIndexed { index, value ->
                peakState.updatePeakPlanFuel(index, value)
            }
        }

        return fresh.copy(
            peakValue = peakState.peakLitersPerLap?.let { "%.2f L".format(Locale.US, it) } ?: "—",
            peakLitersPerLapRaw = peakState.peakLitersPerLap,
            planRows = fresh.planRows.mapIndexed { index, row ->
                val peakLiters = peakState.peakPlanFuelLiters.getOrNull(index)
                row.copy(peakFuelText = peakLiters?.let { "${it.roundToInt()} L" } ?: "—")
            }
        )
    }

    private inline fun updateState(transform: FuelHudUiState.() -> FuelHudUiState) {
        _state.update { it.transform() }
    }
}
