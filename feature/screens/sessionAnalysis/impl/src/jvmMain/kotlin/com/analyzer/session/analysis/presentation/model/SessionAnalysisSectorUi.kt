package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisSectorUi(
    val label: String,
    val startTrackPosition: Float,
    val endTrackPosition: Float,
    val markerTrackPosition: Float,
    val selectedTimeMs: Int? = null,
    val referenceTimeMs: Int? = null,
    val deltaMs: Int? = null,
    val note: String = "",
    val gateCenterX: Float? = null,
    val gateCenterY: Float? = null,
    val gateNormalX: Float? = null,
    val gateNormalY: Float? = null,
    val gateHalfWidthMeters: Float? = null,
)
