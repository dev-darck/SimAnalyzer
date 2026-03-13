package com.project.analyzer.hudSettings.domain.interactor

import com.project.analyzer.hud.api.HudPanel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

interface HudSettingsUseCase {

    fun loadPanels(): ImmutableList<HudPanel>
    fun observeVisiblePanels(): Flow<Set<String>>
    fun observeHudOpacity(): Flow<Float>
    suspend fun updateVisiblePanels(currentVisibleIds: Set<String>, id: String, enable: Boolean)
    suspend fun updateHudOpacity(opacity: Float)
}
