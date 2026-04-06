package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.ui.graphics.Color

/**
 * Palette helpers translate semantic marker roles into concrete map colors.
 */
internal fun SessionAnalysisTrackMapPalette.sectorAccent(label: String): Color = when (label) {
    "SF" -> sectorSfColor
    "S1" -> sectorS1Color
    "S2" -> sectorS2Color
    else -> sectorOtherColor
}
