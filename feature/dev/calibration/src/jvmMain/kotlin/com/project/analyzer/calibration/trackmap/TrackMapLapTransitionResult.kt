package com.project.analyzer.calibration.trackmap

import com.project.analyzer.math.Vec2

data class TrackMapLapTransitionResult(
    val message: String? = null,
    val forcePublish: Boolean = false,
    val completedLapPoints: List<Vec2> = emptyList(),
    val lapAccepted: Boolean = false,
)
