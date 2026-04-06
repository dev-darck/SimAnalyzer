package com.project.analyzer.telemetry.analysis.api.model.report.session

public data class SessionAnalysisCornerZone(
    val cornerNumber: Int,
    val startTrackPosition: Float,
    val endTrackPosition: Float,
    val apexTrackPosition: Float,
    val peakCurvature: Float,
)
