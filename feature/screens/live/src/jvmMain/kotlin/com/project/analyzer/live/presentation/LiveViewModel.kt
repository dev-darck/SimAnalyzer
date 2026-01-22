package com.project.analyzer.live.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.live.domain.model.LiveTelemetryResult
import com.project.analyzer.live.domain.usecase.LiveTelemetryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

internal class LiveViewModel(
    private val useCase: LiveTelemetryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LiveScreenState())
    val state: StateFlow<LiveScreenState> = _state.asStateFlow()

    fun dispatch(intent: LiveIntent) {
        when (intent) {
            is LiveIntent.Start -> startListening()
        }
    }

    private fun startListening() {
        useCase.telemetryFlow
            .onEach { result -> handleResult(result) }
            .launchIn(viewModelScope)
    }

    private fun handleResult(result: LiveTelemetryResult) {
        when (result) {
            is LiveTelemetryResult.SessionEnded -> {
            }

            is LiveTelemetryResult.SessionReset -> {
                _state.update { LiveScreenState() }
            }

            is LiveTelemetryResult.NoData -> {
            }

            is LiveTelemetryResult.Data -> {
                _state.update { result.state }
            }
        }
    }
}
