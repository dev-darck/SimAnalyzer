package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class TrackIssueMarkerPlacement(
    val baseDistancePx: Float,
    val focusDistanceBonusPx: Float,
    val stackStepPx: Float,
    val tangentShiftPx: Float,
    val outwardRetryPx: Float,
    val spacingPx: Float,
    val viewportMarginPx: Float,
)
