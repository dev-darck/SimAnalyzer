package com.project.analyzer.hudSettings.presentation

import androidx.lifecycle.viewModelScope
import com.project.analyzer.hudSettings.domain.interactor.HudSettingsUseCase
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
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
                updateState { useCase.applyVisiblePanels(this, visibleIds) }
            }
        }

        viewModelScope.launch {
            useCase.observeHudOpacity().collect { opacity ->
                updateState { useCase.applyHudOpacity(this, opacity) }
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
                updateState { useCase.toggleSelectedPanel(this, intent.id) }
            }

            is HudSettingsIntent.UpdateHudOpacity -> {
                useCase.updateHudOpacity(intent.opacity)
            }
        }
    }
}
