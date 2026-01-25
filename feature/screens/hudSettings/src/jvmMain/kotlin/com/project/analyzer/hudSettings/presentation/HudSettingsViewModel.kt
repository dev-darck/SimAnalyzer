package com.project.analyzer.hudSettings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.impl.compose.HudPreferences
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HudSettingsViewModel(
    private val preferences: HudPreferences,
    private val panels: Provider<Set<HudPanel>>,
) : ViewModel() {

    private val _state = MutableStateFlow(HudUiState())
    val state: StateFlow<HudUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(panels = panels.invoke().toList()) }
        }

        viewModelScope.launch {
            preferences.observeVisiblePanels().collect { visibleIds ->
                _state.update { current ->
                    val newVisible = visibleIds.associateWith { id ->
                        current.visiblePanels[id] ?: 0
                    }
                    current.copy(visiblePanels = newVisible)
                }
            }
        }
    }

    fun dispatch(intent: HudSettingsIntent) {
        when (intent) {
            is HudSettingsIntent.TogglePanel -> togglePanel(intent.id, intent.enable)

            is HudSettingsIntent.OnShowPanel -> {
                viewModelScope.launch {
                    _state.update {
                        if (it.panel?.id != intent.id) {
                            it.copy(panel = it.panels.find { panel -> panel.id == intent.id })
                        } else {
                            it.copy(panel = null)
                        }
                    }
                }
            }
        }
    }

    private fun togglePanel(id: String, enable: Boolean) {
        viewModelScope.launch {
            if (enable) {
                preferences.saveVisiblePanels(_state.value.visiblePanels.keys + id)
            } else {
                preferences.saveVisiblePanels(_state.value.visiblePanels.keys - id)
            }
        }
    }
}
