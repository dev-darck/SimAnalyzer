package com.project.analyzer.fuel.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.fuel.domain.model.FuelResult
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionConfig
import com.project.analyzer.fuel.domain.usecase.FuelConsumptionUseCase
import com.project.analyzer.fuel.presentation.FuelHudUiState
import com.project.analyzer.fuel.presentation.map.toUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

internal class FuelHudViewModel(
    private val useCase: FuelConsumptionUseCase,
    private val cfg: FuelConsumptionConfig
) : ViewModel() {

    private val safetyFactor: Double get() = 1.0 + (cfg.safetyMarginPercent / 100.0)

    private var estimateJob: Job? = null

    private var currentKey: SessionKey = SessionKey(null, null)
    private val peaksByKey: MutableMap<SessionKey, PeakState> = mutableMapOf()

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
        estimateJob?.cancel()
        estimateJob = useCase.fuelEstimates
            .sample(200.milliseconds)
            .onEach { result -> handleFuelResult(result) }
            .launchIn(viewModelScope)
    }

    private fun handleFuelResult(result: FuelResult) {
        when (result) {
            is FuelResult.SessionEnded -> {
                _state.update {
                    it.copy(
                        isShow = false,
                        isSessionActive = false
                    )
                }
            }

            is FuelResult.Reset -> {
                peaksByKey[currentKey]?.reset()
                _state.update {
                    it.copy(
                        isShow = true,
                        isSessionActive = true
                    )
                }
            }

            is FuelResult.NoData -> {
                _state.update {
                    it.copy(
                        isShow = true,
                        isSessionActive = true
                    )
                }
            }

            is FuelResult.Data -> {
                val estimate = result.estimate

                val newKey = SessionKey(estimate.carModel, estimate.trackId)
                if (newKey != currentKey) {
                    currentKey = newKey
                }

                val baseUiState = estimate.toUiState(cfg, safetyFactor)
                val withPeaks = applyPeakTracking(baseUiState)

                _state.update {
                    withPeaks.copy(
                        isShow = true,
                        isSessionActive = true
                    )
                }
            }
        }
    }

    private fun applyPeakTracking(fresh: FuelHudUiState): FuelHudUiState {
        val ps = peaksByKey.getOrPut(currentKey) { PeakState() }

        val canUpdatePeak = fresh.phase == com.project.analyzer.fuel.domain.model.FuelPhase.PER_LAP &&
            fresh.isCurrentLapValid

        if (canUpdatePeak) {
            ps.updatePeakLitersPerLap(fresh.litersPerLapRaw)

            fresh.planFuelLitersRaw.forEachIndexed { index, value ->
                ps.updatePeakPlanFuel(index, value)
            }
        }

        val peakValue = ps.peakLitersPerLap?.let {
            String.format(Locale.US, "%.2f L", it)
        } ?: "—"

        val planRowsWithPeaks = fresh.planRows.mapIndexed { index, row ->
            val peakLiters = ps.peakPlanFuelLiters.getOrNull(index)
            row.copy(
                peakFuelText = peakLiters?.let { "${it.roundToInt()} L" } ?: "—"
            )
        }

        return fresh.copy(
            peakValue = peakValue,
            peakLitersPerLapRaw = ps.peakLitersPerLap,
            planRows = planRowsWithPeaks
        )
    }

    override fun onCleared() {
        super.onCleared()
        estimateJob?.cancel()
    }
}
