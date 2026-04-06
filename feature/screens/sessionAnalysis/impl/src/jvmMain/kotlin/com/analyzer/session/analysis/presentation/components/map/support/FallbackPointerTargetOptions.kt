package com.analyzer.session.analysis.presentation.components.map.support

internal data class FallbackPointerTargetOptions(
    val hoverDistancePx: Float,
    val hoverPreferenceFraction: Float?,
    val focusMode: Boolean,
    val purpose: SessionAnalysisTrackMapPointerPurpose,
)
