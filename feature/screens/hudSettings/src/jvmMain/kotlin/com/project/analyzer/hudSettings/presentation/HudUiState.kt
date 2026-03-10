package com.project.analyzer.hudSettings.presentation

import com.project.analyzer.hud.api.DEFAULT_HUD_BACKGROUND_OPACITY
import com.project.analyzer.hud.api.HudPanel

data class HudUiState(
    val visiblePanels: Map<String, Int> = emptyMap(),
    val panels: List<HudPanel> = emptyList(),
    val panel: HudPanel? = null,
    val hudOpacity: Float = DEFAULT_HUD_BACKGROUND_OPACITY,
)
