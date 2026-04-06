package com.analyzer.session.analysis.presentation.components.map.model

internal data class SessionAnalysisTrackMapHitSearchOptions(
    val maxDistanceSquared: Float,
    val preferredFraction: Float?,
    val fractionWindow: Float?,
    val preferredIndex: Int?,
    val indexWindow: Int?,
    val jumpThreshold: Float?,
)
