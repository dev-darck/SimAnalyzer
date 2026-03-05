package com.project.analyzer.hudSettings.domain.interactor

import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hudSettings.presentation.HudUiState
import kotlinx.coroutines.flow.Flow

interface HudSettingsUseCase {

    fun loadPanels(): List<HudPanel>
    fun observeVisiblePanels(): Flow<Set<String>>
    fun observeHudOpacity(): Flow<Float>
    suspend fun updateVisiblePanels(currentVisibleIds: Set<String>, id: String, enable: Boolean)
    suspend fun updateHudOpacity(opacity: Float)
    fun applyVisiblePanels(state: HudUiState, visibleIds: Set<String>): HudUiState
    fun applyHudOpacity(state: HudUiState, opacity: Float): HudUiState
    fun toggleSelectedPanel(state: HudUiState, id: String): HudUiState
}
