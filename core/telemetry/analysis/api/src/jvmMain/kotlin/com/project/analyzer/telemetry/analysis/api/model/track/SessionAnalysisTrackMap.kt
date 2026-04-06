package com.project.analyzer.telemetry.analysis.api.model.track
public data class SessionAnalysisTrackMap(
    val points: List<SessionAnalysisTrackMapPoint> = emptyList(),
    val pitPoints: List<SessionAnalysisTrackMapPoint> = emptyList(),
    val idealPoints: List<SessionAnalysisTrackMapPoint> = emptyList(),
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
)
