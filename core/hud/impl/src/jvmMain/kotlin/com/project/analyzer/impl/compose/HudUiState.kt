package com.project.analyzer.impl.compose

import com.project.analyzer.hud.api.DefaultHudBackgroundOpacity
import com.project.analyzer.hud.api.HudStoredPosition

data class HudUiState(
    val visiblePanels: Map<String, Int> = emptyMap(),
    val positions: Map<String, HudStoredPosition> = emptyMap(),
    val inputLocked: Boolean = false,
    val hudOpacity: Float = DefaultHudBackgroundOpacity,
)
