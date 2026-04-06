package com.analyzer.session.analysis.presentation.builder.track

/**
 * Holds the resolved left and right edge polylines that define the drawn track corridor.
 */
internal data class TrackEdgeGeometry(
    val points: List<com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackPointUi>,
    val fractions: List<Float>,
    val defaultHalfWidth: Float,
    val isClosedLoop: Boolean,
)
