package com.analyzer.session.analysis.presentation.builder.coach

internal data class BrakeZone(
    val startTrackPosition: Float,
    val peakTrackPosition: Float,
    val endTrackPosition: Float,
    val minimumSpeedKmh: Float? = null,
    val fullThrottleTrackPosition: Float? = null,
)
