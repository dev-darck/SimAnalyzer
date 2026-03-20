package com.analyzer.trackmap.data.live.geometry

import com.project.analyzer.math.Vec2

internal data class TrackMapLivePositionProjection(
    val point: Vec2,
    val distanceToTrack: Float,
)
