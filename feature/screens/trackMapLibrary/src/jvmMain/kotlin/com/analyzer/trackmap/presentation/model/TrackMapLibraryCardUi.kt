package com.analyzer.trackmap.presentation.model

import com.project.analyzer.math.Vec2

data class TrackMapLibraryCardUi(
    val mapKey: String,
    val gameId: String,
    val trackId: String,
    val trackName: String,
    val layoutId: String?,
    val createdAtEpochMs: Long,
    val pointCount: Int,
    val points: List<Vec2>,
    val distanceMeters: Float,
    val pitPointCount: Int,
    val preview: TrackMapPreviewUi,
    val averageTrackWidthMeters: Float,
)
