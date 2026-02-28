package com.project.analyzer.impl.compose

import androidx.lifecycle.viewModelScope
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class HudViewModel(private val useCase: HudContainerUseCase) :
    LeakAwareMviViewModel<HudIntent, HudUiState>(HudUiState()) {

    init {
        useCase.observeVisiblePanels()
            .onEach { visibleIds ->
                updateState { useCase.applyVisiblePanels(this, visibleIds) }
            }
            .launchIn(viewModelScope)

        useCase.observeInputLocked()
            .onEach { locked ->
                updateState { useCase.applyInputLocked(this, locked) }
            }
            .launchIn(viewModelScope)

        useCase.observeHudOpacity()
            .onEach { opacity ->
                updateState { useCase.applyHudOpacity(this, opacity) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val positions = useCase.loadPositions()
            updateState { useCase.applyPositions(this, positions) }
        }
    }

    override suspend fun handleIntent(intent: HudIntent) {
        when (intent) {
            is HudIntent.Show -> {
                useCase.saveVisiblePanels(currentState.visiblePanels.keys + intent.id)
            }

            is HudIntent.Hide -> {
                useCase.saveVisiblePanels(currentState.visiblePanels.keys - intent.id)
            }

            is HudIntent.Toggle -> {
                val next = if (intent.id in currentState.visiblePanels) {
                    currentState.visiblePanels.keys - intent.id
                } else {
                    currentState.visiblePanels.keys + intent.id
                }
                useCase.saveVisiblePanels(next)
            }

            is HudIntent.Restart -> {
                updateState { useCase.restartPanel(this, intent.id) }
            }

            HudIntent.HideAll -> {
                useCase.saveVisiblePanels(emptySet())
            }

            is HudIntent.SavePosition -> {
                updateState { useCase.savePositionLocally(this, intent.id, intent.position) }
                useCase.savePosition(intent.id, intent.position)
            }

            HudIntent.ToggleInputLock -> {
                val nextState = useCase.toggleInputLock(currentState)
                setState(nextState)
                useCase.saveInputLocked(nextState.inputLocked)
            }
        }
    }
}
