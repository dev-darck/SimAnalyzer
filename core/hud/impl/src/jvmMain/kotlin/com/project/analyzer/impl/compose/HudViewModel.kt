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
            preferences.observeVisiblePanels().collect { visibleIds ->
                _state.update { current ->
                    val newVisiblePanels = visibleIds.associateWith { id ->
                        current.visiblePanels[id] ?: 0
                    }
                    current.copy(visiblePanels = newVisiblePanels)
                }
            }
        }
        viewModelScope.launch {
            loadPositions()
        }
    }

    fun dispatch(intent: HudIntent) {
        when (intent) {
            is HudIntent.Show -> showPanel(intent.id)
            is HudIntent.Hide -> hidePanel(intent.id)
            is HudIntent.Toggle -> togglePanel(intent.id)
            is HudIntent.Restart -> restartPanel(intent.id)
            is HudIntent.HideAll -> hideAllPanels()
            is HudIntent.SavePosition -> savePosition(intent.id, intent.offset)
        }
    }

    private fun showPanel(id: String) {
        viewModelScope.launch {
            val currentIds = _state.value.visiblePanels.keys
            preferences.saveVisiblePanels(currentIds + id)
        }
    }

    private fun hidePanel(id: String) {
        viewModelScope.launch {
            val currentIds = _state.value.visiblePanels.keys
            preferences.saveVisiblePanels(currentIds - id)
        }
    }

    private fun togglePanel(id: String) {
        if (id in _state.value.visiblePanels) {
            hidePanel(id)
        } else {
            showPanel(id)
        }
    }

    private fun restartPanel(id: String) {
        _state.update { current ->
            if (id in current.visiblePanels) {
                val newVersion = (current.visiblePanels[id] ?: 0) + 1
                current.copy(
                    visiblePanels = current.visiblePanels + (id to newVersion)
                )
            } else {
                current
            }
        }
    }

    private fun hideAllPanels() {
        viewModelScope.launch {
            preferences.saveVisiblePanels(emptySet())
        }
    }

    private fun savePosition(id: String, offset: IntOffset) {
        _state.update { current ->
            current.copy(positions = current.positions + (id to offset))
        }
        viewModelScope.launch {
            preferences.savePosition(id, offset)
        }
    }

    private suspend fun loadPositions() {
        val positions = preferences.getVisiblePanelsPositions()
        _state.update { current ->
            current.copy(positions = positions)
        }
    }
}
