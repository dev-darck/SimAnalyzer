package com.project.analyzer.hudSettings.domain.interactor

import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hudSettings.presentation.HudUiState
import com.project.analyzer.impl.compose.HudPreferencesRepository
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.flow.Flow

@Inject
class HudSettingsUseCase(
    private val hudRepository: HudPreferencesRepository,
    private val panels: Provider<Set<HudPanel>>,
) {

    fun loadPanels(): List<HudPanel> = panels.invoke()
        .filter { panel -> !panel.isDevOnly }
        .sortedBy { panel -> panel.id }

    fun observeVisiblePanels(): Flow<Set<String>> = hudRepository.observeVisiblePanels()

    fun observeHudOpacity(): Flow<Float> = hudRepository.observeHudOpacity()

    suspend fun updateVisiblePanels(currentVisibleIds: Set<String>, id: String, enable: Boolean) {
        val next = if (enable) currentVisibleIds + id else currentVisibleIds - id
        hudRepository.saveVisiblePanels(next)
    }

    suspend fun updateHudOpacity(opacity: Float) {
        hudRepository.setHudOpacity(opacity.coerceIn(0f, 1f))
    }

    fun applyVisiblePanels(state: HudUiState, visibleIds: Set<String>): HudUiState {
        val newVisible = visibleIds.associateWith { id -> state.visiblePanels[id] ?: 0 }
        return state.copy(visiblePanels = newVisible)
    }

    fun applyHudOpacity(state: HudUiState, opacity: Float): HudUiState = state.copy(
        hudOpacity = opacity.coerceIn(0f, 1f),
    )

    fun toggleSelectedPanel(state: HudUiState, id: String): HudUiState = state.copy(
        panel = if (state.panel?.id != id) {
            state.panels.find { panel -> panel.id == id }
        } else {
            null
        },
    )
}
