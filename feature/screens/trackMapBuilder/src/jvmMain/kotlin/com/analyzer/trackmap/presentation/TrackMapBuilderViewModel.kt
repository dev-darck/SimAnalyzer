package com.analyzer.trackmap.presentation

import androidx.lifecycle.viewModelScope
import com.analyzer.trackmap.domain.usecase.TrackMapBuilderUseCase
import com.analyzer.trackmap.presentation.model.TrackMapBuilderUiState
import com.project.analyzer.leak.api.LeakAwareViewModel
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
internal class TrackMapBuilderViewModel(private val useCase: TrackMapBuilderUseCase) : LeakAwareViewModel() {

    private val _state = MutableStateFlow(TrackMapBuilderUiState())
    val state: StateFlow<TrackMapBuilderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            useCase.observeState().collect { currentState ->
                _state.value = currentState.toTrackMapBuilderUiState()
            }
        }
    }

    fun start() {
        useCase.start()
    }

    fun stop() {
        useCase.stop()
    }

    fun reset() {
        useCase.reset()
    }

    fun setReferencePoint(point: ReferencePoint) {
        useCase.setReferencePoint(point)
    }

    fun setFallbackHalfWidthMeters(value: Float) {
        useCase.setFallbackHalfWidthMeters(value)
    }

    fun markPitEntry() {
        useCase.markPitEntry()
    }

    fun markPitExit() {
        useCase.markPitExit()
    }

    fun save() {
        viewModelScope.launch {
            useCase.save()
        }
    }
}
