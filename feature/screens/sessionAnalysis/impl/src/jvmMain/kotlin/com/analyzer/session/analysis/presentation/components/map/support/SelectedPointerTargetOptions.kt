package com.analyzer.session.analysis.presentation.components.map.support

internal data class SelectedPointerTargetOptions(
    val hoverDistancePx: Float,
    val hoverPreferenceIndex: Int?,
    val hoverLocalIndexWindow: Int,
    val focusMode: Boolean,
    val purpose: SessionAnalysisTrackMapPointerPurpose,
)
