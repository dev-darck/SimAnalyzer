package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

/**
 * Represents a single point on the track map.
 */
@Immutable
internal data class SessionAnalysisTrackPointUi(
    val x: Float,
    val y: Float,
    val leftWidthMeters: Float? = null,
    val rightWidthMeters: Float? = null,
)
