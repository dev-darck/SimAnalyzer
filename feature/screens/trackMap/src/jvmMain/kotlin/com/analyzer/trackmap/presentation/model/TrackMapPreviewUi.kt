package com.analyzer.trackmap.presentation.model

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds

data class TrackMapPreviewUi(
    val recording: Boolean = false,
    val points: List<Vec2> = emptyList(),
    val leftWidthsMeters: List<Float> = emptyList(),
    val rightWidthsMeters: List<Float> = emptyList(),
    val pointCount: Int = 0,
    val totalDistanceMeters: Float = 0f,
    val bounds: TrackMapBounds? = null,
    val currentPosition: Vec2? = null,
    val pitPoints: List<Vec2> = emptyList(),
    val pitEntryPoint: Vec2? = null,
    val pitExitPoint: Vec2? = null,
    val sectorMarkerPositions: List<Vec2> = emptyList(),
    val averageTrackWidthMeters: Float = 0f,
    val guidanceText: String? = null,
)
