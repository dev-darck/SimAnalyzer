package com.project.analyzer.impl.compose

import com.project.analyzer.hud.api.DEFAULT_HUD_BACKGROUND_OPACITY
import com.project.analyzer.hud.api.HudStoredPosition
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

data class HudUiState(
    val visiblePanels: ImmutableMap<String, Int> = persistentMapOf(),
    val positions: ImmutableMap<String, HudStoredPosition> = persistentMapOf(),
    val inputLocked: Boolean = false,
    val hudOpacity: Float = DEFAULT_HUD_BACKGROUND_OPACITY,
)
