package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class TrackCornerMarkerPlacement(
    val edgeOffsetPx: Float,
    val baseDistancePx: Float,
    val spacingPx: Float,
    val tangentRetryPx: Float,
    val outwardRetryPx: Float,
    val viewportMarginPx: Float,
)
