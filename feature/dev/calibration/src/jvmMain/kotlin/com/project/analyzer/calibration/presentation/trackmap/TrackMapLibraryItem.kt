package com.project.analyzer.calibration.presentation.trackmap

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds

data class TrackMapLibraryItem(
    val map: TrackMap,
    val points: List<Vec2>,
    val leftWidthsMeters: List<Float>,
    val rightWidthsMeters: List<Float>,
    val pitPoints: List<Vec2>,
    val bounds: TrackMapBounds?,
    val distanceMeters: Float,
    val pitEntryPoint: Vec2?,
    val pitExitPoint: Vec2?
)
