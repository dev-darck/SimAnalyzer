package com.analyzer.trackmap.data.live.model

import com.analyzer.trackmap.data.live.geometry.TrackMapLivePositionProjection
import com.project.analyzer.math.Vec2

internal data class TrackMapLivePositionCandidates(
    val worldPosition: Vec2?,
    val wheelReferencePosition: Vec2?,
    val rawPosition: Vec2?,
    val rawProjection: TrackMapLivePositionProjection?,
    val acceptedPosition: Vec2?,
    val lapProgressPosition: Vec2?,
    val geometryFallbackPosition: Vec2?,
)
