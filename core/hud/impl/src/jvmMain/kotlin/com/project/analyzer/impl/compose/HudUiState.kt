package com.project.analyzer.impl.compose

import androidx.compose.ui.unit.IntOffset

data class HudUiState(
    val visiblePanels: Map<String, Int> = emptyMap(),
    val positions: Map<String, IntOffset> = emptyMap(),
    val inputLocked: Boolean = false,
)
