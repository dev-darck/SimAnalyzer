package com.project.analyzer.hudSettings.presentation

import androidx.lifecycle.viewModelScope
import com.project.analyzer.hudSettings.domain.interactor.HudSettingsUseCase
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.launch

@Inject
internal class HudSettingsViewModel(private val useCase: HudSettingsUseCase) :
    LeakAwareMviViewModel<HudSettingsIntent, HudUiState>(HudUiState()) {

    init {
        viewModelScope.launch {
            val panels = useCase.loadPanels()
            updateState { copy(panels = panels) }
        }

        viewModelScope.launch {
            useCase.observeVisiblePanels().collect { visibleIds ->
                updateState {
                    copy(
                        visiblePanels = visibleIds
                            .associateWith { id -> visiblePanels[id] ?: 0 }
                            .toImmutableMap(),
                    )
                }
            }
        }

        viewModelScope.launch {
            useCase.observeHudOpacity().collect { opacity ->
                updateState { copy(hudOpacity = opacity.coerceIn(0f, 1f)) }
            }
        }
    }

    override suspend fun handleIntent(intent: HudSettingsIntent) {
        when (intent) {
            is HudSettingsIntent.TogglePanel -> {
                useCase.updateVisiblePanels(
                    currentVisibleIds = currentState.visiblePanels.keys,
                    id = intent.id,
                    enable = intent.enable,
                )
            }

            is HudSettingsIntent.OnShowPanel -> {
                updateState {
                    copy(
                        panel = if (panel?.id != intent.id) {
                            panels.find { panel -> panel.id == intent.id }
                        } else {
                            null
                        },
                    )
                }
            }

            is HudSettingsIntent.UpdateHudOpacity -> {
                useCase.updateHudOpacity(intent.opacity)
            }
        }
    }
}
