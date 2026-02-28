package com.project.analyzer.live.presentation

import androidx.lifecycle.viewModelScope
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import com.project.analyzer.live.domain.mapper.LiveScreenStateMapper
import com.project.analyzer.live.domain.model.LiveTelemetryResult
import com.project.analyzer.live.domain.usecase.LiveTelemetryUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Inject
internal class LiveViewModel(
    private val useCase: LiveTelemetryUseCase,
    private val uiStateMapper: LiveScreenStateMapper,
) : LeakAwareMviViewModel<LiveIntent, LiveScreenState>(LiveScreenState()) {

    private var telemetryJob: Job? = null

    override suspend fun handleIntent(intent: LiveIntent) {
        when (intent) {
            LiveIntent.Start -> startListening()
        }
    }

    private fun startListening() {
        if (telemetryJob?.isActive == true) return
        telemetryJob?.cancel()
        telemetryJob = useCase.telemetryFlow
            .onEach(::handleResult)
            .launchIn(viewModelScope)
    }

    private fun handleResult(result: LiveTelemetryResult) {
        when (result) {
            is LiveTelemetryResult.SessionEnded -> Unit

            LiveTelemetryResult.SessionReset -> setState(LiveScreenState())

            is LiveTelemetryResult.Data -> {
                uiStateMapper.map(result.frame)?.let(::setState)
            }
        }
    }
}
