package com.project.analyzer.hudSettings.domain.interactor

import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.HudPreferencesStore
import com.project.analyzer.hudSettings.presentation.HudUiState
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.flow.Flow

class HudSettingsUseCaseImpl(
    private val hudPreferencesStore: HudPreferencesStore,
    private val panels: Provider<Set<HudPanel>>,
) : HudSettingsUseCase {

    override fun loadPanels(): List<HudPanel> = panels.invoke()
        .filter { panel -> !panel.isDevOnly }
        .sortedBy { panel -> panel.id }

    override fun observeVisiblePanels(): Flow<Set<String>> = hudPreferencesStore.observeVisiblePanels()

    override fun observeHudOpacity(): Flow<Float> = hudPreferencesStore.observeHudOpacity()

    override suspend fun updateVisiblePanels(currentVisibleIds: Set<String>, id: String, enable: Boolean) {
        val next = if (enable) currentVisibleIds + id else currentVisibleIds - id
        hudPreferencesStore.saveVisiblePanels(next)
    }

    override suspend fun updateHudOpacity(opacity: Float) {
        hudPreferencesStore.setHudOpacity(opacity.coerceIn(0f, 1f))
    }

    override fun applyVisiblePanels(state: HudUiState, visibleIds: Set<String>): HudUiState {
        val newVisible = visibleIds.associateWith { id -> state.visiblePanels[id] ?: 0 }
        return state.copy(visiblePanels = newVisible)
    }

    override fun applyHudOpacity(state: HudUiState, opacity: Float): HudUiState = state.copy(
        hudOpacity = opacity.coerceIn(0f, 1f),
    )

    override fun toggleSelectedPanel(state: HudUiState, id: String): HudUiState = state.copy(
        panel = if (state.panel?.id != id) {
            state.panels.find { panel -> panel.id == id }
        } else {
            null
        },
    )
}
