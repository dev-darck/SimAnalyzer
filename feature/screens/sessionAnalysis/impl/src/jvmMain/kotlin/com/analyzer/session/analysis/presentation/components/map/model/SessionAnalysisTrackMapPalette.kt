package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class SessionAnalysisTrackMapPalette(
    val panelBg: Color,
    val canvasBg: Color,
    val trackSurfaceColor: Color,
    val trackEdgeColor: Color,
    val lineCasingColor: Color,
    val referenceColor: Color,
    val idealColor: Color,
    val selectedColor: Color,
    val markerRingColor: Color,
    val markerInnerColor: Color,
    val overlayBg: Color,
    val overlayFg: Color,
    val overlayDimAlpha: Float,
    val sectorLabelBg: Color,
    val sectorSfColor: Color,
    val sectorS1Color: Color,
    val sectorS2Color: Color,
    val sectorOtherColor: Color,
)
