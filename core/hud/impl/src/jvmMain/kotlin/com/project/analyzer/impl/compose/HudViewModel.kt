package com.project.analyzer.impl.compose

import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HudViewModel(
    private val preferences: HudPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(HudUiState())
    val state: StateFlow<HudUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.observeVisiblePanels().collect { visible ->
                _state.update { current ->
                    val newMap = visible.associateWith { id ->
                        current.visiblePanels[id] ?: 0
                    }
                    current.copy(visiblePanels = newMap)
                }
            }
        }
        viewModelScope.launch { loadPositions() }
    }

    fun dispatch(intent: HudIntent) {
        when (intent) {
            is HudIntent.Show -> onShow(intent.id)
            is HudIntent.Hide -> onHide(intent.id)
            is HudIntent.Toggle -> onToggle(intent.id)
            is HudIntent.Restart -> onRestart(intent.id)
            HudIntent.HideAll -> onHideAll()
            is HudIntent.UpdatePosition -> onUpdatePosition(intent.id, intent.offset)
            is HudIntent.SavePosition -> onSavePosition(intent.id)
        }
    }

    private fun onShow(id: String) {
        viewModelScope.launch {
            preferences.saveVisiblePanels(_state.value.visiblePanels.keys + id)
        }
    }

    private fun onHide(id: String) {
        viewModelScope.launch {
            preferences.saveVisiblePanels(_state.value.visiblePanels.keys - id)
        }
    }

    private fun onToggle(id: String) {
        if (id in _state.value.visiblePanels) onHide(id) else onShow(id)
    }

    private fun onRestart(id: String) {
        _state.update { current ->
            if (id in current.visiblePanels) {
                current.copy(
                    visiblePanels = current.visiblePanels + (id to (current.visiblePanels[id] ?: 0) + 1)
                )
            } else current
        }
    }

    private fun onHideAll() {
        viewModelScope.launch {
            preferences.saveVisiblePanels(emptySet())
        }
    }

    private fun onUpdatePosition(id: String, offset: IntOffset) {
        _state.update { current ->
            current.copy(positions = current.positions + (id to offset))
        }
    }

    private fun loadPositions() {
        viewModelScope.launch {
            val positions = preferences.getVisiblePanelsPositions()
            _state.update { current ->
                current.copy(positions = positions)
            }
        }
    }

    private fun onSavePosition(id: String) {
        val offset = _state.value.positions[id] ?: return
        viewModelScope.launch {
            preferences.savePosition(id, offset)
        }
    }
}
