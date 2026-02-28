package com.project.analyzer.hudSettings.presentation

import com.project.analyzer.hud.api.DefaultHudBackgroundOpacity
import com.project.analyzer.hud.api.HudPanel

data class HudUiState(
    val visiblePanels: Map<String, Int> = emptyMap(),
    val panels: List<HudPanel> = emptyList(),
    val panel: HudPanel? = null,
    val hudOpacity: Float = DefaultHudBackgroundOpacity,
)
