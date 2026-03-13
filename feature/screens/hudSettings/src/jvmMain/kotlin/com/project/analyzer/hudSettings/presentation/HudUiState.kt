package com.project.analyzer.hudSettings.presentation

import com.project.analyzer.hud.api.DEFAULT_HUD_BACKGROUND_OPACITY
import com.project.analyzer.hud.api.HudPanel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class HudUiState(
    val visiblePanels: ImmutableMap<String, Int> = persistentMapOf(),
    val panels: ImmutableList<HudPanel> = persistentListOf(),
    val panel: HudPanel? = null,
    val hudOpacity: Float = DEFAULT_HUD_BACKGROUND_OPACITY,
)
