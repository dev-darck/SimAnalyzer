package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

@Immutable
internal data class SessionAnalysisTrackMapCameraState(
    val anchor: Offset?,
    val focusPoint: Offset?,
    val zoom: Float,
    val canvasSize: IntSize,
)
