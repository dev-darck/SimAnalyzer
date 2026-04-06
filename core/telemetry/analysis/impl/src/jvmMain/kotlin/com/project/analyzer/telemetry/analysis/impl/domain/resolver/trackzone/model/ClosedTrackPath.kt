package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.model

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint

internal data class ClosedTrackPath(
    val points: List<SessionAnalysisTrackMapPoint>,
    val segmentLengths: List<Float>,
    val totalDistance: Float,
)
