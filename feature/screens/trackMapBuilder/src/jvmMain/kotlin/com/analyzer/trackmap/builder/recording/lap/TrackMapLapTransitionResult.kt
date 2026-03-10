package com.analyzer.trackmap.builder.recording.lap

import com.project.analyzer.math.Vec2

internal data class TrackMapLapTransitionResult(
    val message: String? = null,
    val forcePublish: Boolean = false,
    val completedLapPoints: List<Vec2> = emptyList(),
    val lapAccepted: Boolean = false,
)
