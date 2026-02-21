package com.project.analyzer.inputs.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.inputs.domain.model.InputsResult
import com.project.analyzer.inputs.domain.usecase.InputsUseCase
import com.project.analyzer.inputs.presentation.model.InputsSeries
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

@Inject
internal class InputsHudViewModel(private val useCase: InputsUseCase) : ViewModel() {

    private var steerPeakAbs: Float = 0.35f
    private val steerPeakDecay: Float = 0.995f
    private val steerPeakMin: Float = 0.10f

    private val _state = MutableStateFlow(InputsHudUiState())
    val state: StateFlow<InputsHudUiState> = _state.asStateFlow()

    private var telemetryJob: Job? = null

    init {
        useCase
            .settings
            .distinctUntilChanged()
            .onEach {
                _state.update { state ->
                    val updated = state.copy(settings = it)
                    updated.copy(series = applyHistoryCapacityIfNeeded(updated))
                }
            }
            .launchIn(viewModelScope)
    }

    fun dispatch(intent: InputsIntent) {
        when (intent) {
            InputsIntent.Start -> subscribe()

            is InputsIntent.UpdateSettings -> viewModelScope.launch {
                useCase.updateSettings(intent.inputHudSettings)
            }
        }
    }

    private fun subscribe() {
        telemetryJob?.cancel()
        telemetryJob = useCase.results
            .conflate()
            .onEach(::handle)
            .launchIn(viewModelScope)
    }

    private fun handle(result: InputsResult) {
        when (result) {
            is InputsResult.SessionStarted -> {
                resetSessionDerivedState()

                _state.update { st ->
                    val series = newEmptySeriesFor(st)
                    st.copy(
                        isShow = true,
                        isSessionActive = true,
                        throttle = 0f,
                        brake = 0f,
                        clutch = 0f,
                        steerNorm = 0f,
                        series = series,
                        renderTick = nextTick(st.renderTick),
                    )
                }
            }

            is InputsResult.SessionResumed -> {
                _state.update { st ->
                    st.copy(
                        isShow = true,
                        isSessionActive = true,
                        renderTick = nextTick(st.renderTick),
                    )
                }
            }

            is InputsResult.SessionPaused -> {
                _state.update { st ->
                    st.copy(
                        isShow = false,
                        isSessionActive = false,
                        renderTick = nextTick(st.renderTick),
                    )
                }
            }

            is InputsResult.SessionEnded -> {
                resetSessionDerivedState()
                _state.update { st ->
                    st.copy(
                        isShow = false,
                        isSessionActive = false,
                        renderTick = nextTick(st.renderTick),
                    )
                }
            }

            is InputsResult.Sample -> {
                val t = result.throttle.coerceIn(0f, 1f)
                val b = result.brake.coerceIn(0f, 1f)
                val c = result.clutch.coerceIn(0f, 1f)

                val steer = -result.steerRadians
                val steerAbs = abs(steer)

                steerPeakAbs = max(steerPeakMin, max(steerAbs, steerPeakAbs * steerPeakDecay))
                val steerNorm = (steer / steerPeakAbs).coerceIn(-1f, 1f)

                _state.update { st ->
                    val series = applyHistoryCapacityIfNeeded(st)
                    series.push(t, b, c, steerNorm)

                    st.copy(
                        isShow = true,
                        isSessionActive = true,
                        throttle = t,
                        brake = b,
                        clutch = c,
                        steerNorm = steerNorm,
                        series = series,
                        renderTick = nextTick(st.renderTick),
                    )
                }
            }
        }
    }

    private fun resetSessionDerivedState() {
        steerPeakAbs = 0.35f
    }

    private fun applyHistoryCapacityIfNeeded(state: InputsHudUiState): InputsSeries {
        val seconds = state.settings.historySeconds.coerceIn(1, 3)
        val targetCap = (seconds * UI_SAMPLES_PER_SECOND).coerceIn(60, 2000)
        if (state.series.capacity == targetCap) return state.series
        return state.series.resizedCopy(targetCap)
    }

    private fun newEmptySeriesFor(state: InputsHudUiState): InputsSeries {
        val seconds = state.settings.historySeconds.coerceIn(1, 3)
        val targetCap = (seconds * UI_SAMPLES_PER_SECOND).coerceIn(60, 2000)
        return InputsSeries(capacity = targetCap)
    }

    private fun nextTick(tick: Long): Long = if (tick >= Long.MAX_VALUE - 1) 0L else tick + 1

    private companion object {

        const val UI_SAMPLES_PER_SECOND: Int = 240
    }
}
