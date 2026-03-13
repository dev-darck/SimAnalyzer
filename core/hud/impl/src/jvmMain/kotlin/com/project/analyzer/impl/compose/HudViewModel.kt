package com.project.analyzer.impl.compose

import androidx.lifecycle.viewModelScope
import com.project.analyzer.hud.api.HudStoredPosition
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class HudViewModel(private val useCase: HudContainerUseCase) :
    LeakAwareMviViewModel<HudIntent, HudUiState>(HudUiState()) {

    init {
        useCase.observeVisiblePanels()
            .onEach { visibleIds ->
                updateState { withVisiblePanels(visibleIds) }
            }
            .launchIn(viewModelScope)

        useCase.observeInputLocked()
            .onEach { locked ->
                updateState { copy(inputLocked = locked) }
            }
            .launchIn(viewModelScope)

        useCase.observeHudOpacity()
            .onEach { opacity ->
                updateState { copy(hudOpacity = opacity.coerceIn(0f, 1f)) }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val positions = useCase.loadPositions()
            updateState { copy(positions = positions.toImmutableMap()) }
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
                updateState { restartPanel(intent.id) }
            }

            HudIntent.HideAll -> {
                useCase.saveVisiblePanels(emptySet())
            }

            is HudIntent.SavePosition -> {
                updateState { withSavedPosition(intent.id, intent.position) }
                useCase.savePosition(intent.id, intent.position)
            }

            HudIntent.ToggleInputLock -> {
                val nextState = currentState.toggleInputLock()
                setState(nextState)
                useCase.saveInputLocked(nextState.inputLocked)
            }
        }
    }
}

private fun HudUiState.withVisiblePanels(visibleIds: Set<String>): HudUiState {
    val newVisiblePanels = visibleIds.associateWith { id -> visiblePanels[id] ?: 0 }.toImmutableMap()
    return copy(visiblePanels = newVisiblePanels)
}

private fun HudUiState.toggleInputLock(): HudUiState = copy(inputLocked = !inputLocked)

private fun HudUiState.restartPanel(id: String): HudUiState = copy(
    visiblePanels = if (id in visiblePanels) {
        val newVersion = (visiblePanels[id] ?: 0) + 1
        visiblePanels.toPersistentMap().put(id, newVersion)
    } else {
        visiblePanels
    },
)

private fun HudUiState.withSavedPosition(id: String, position: HudStoredPosition): HudUiState =
    copy(positions = positions.toPersistentMap().put(id, position))
