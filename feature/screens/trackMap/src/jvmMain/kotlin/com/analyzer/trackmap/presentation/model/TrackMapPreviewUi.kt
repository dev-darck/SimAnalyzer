package com.analyzer.trackmap.presentation.model

import androidx.compose.runtime.Immutable
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class TrackMapPreviewUi(
    val recording: Boolean = false,
    val points: ImmutableList<TrackMapPreviewPointUi> = persistentListOf(),
    val leftWidthsMeters: ImmutableList<Float> = persistentListOf(),
    val rightWidthsMeters: ImmutableList<Float> = persistentListOf(),
    val pointCount: Int = 0,
    val totalDistanceMeters: Float = 0f,
    val bounds: TrackMapPreviewBoundsUi? = null,
    val currentPosition: TrackMapPreviewPointUi? = null,
    val pitPoints: ImmutableList<TrackMapPreviewPointUi> = persistentListOf(),
    val pitEntryPoint: TrackMapPreviewPointUi? = null,
    val pitExitPoint: TrackMapPreviewPointUi? = null,
    val sectorMarkerPositions: ImmutableList<TrackMapPreviewPointUi> = persistentListOf(),
    val averageTrackWidthMeters: Float = 0f,
    val guidanceText: String? = null,
)

@Immutable
data class TrackMapPreviewPointUi(val x: Float, val y: Float)

@Immutable
data class TrackMapPreviewBoundsUi(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

fun Vec2.toTrackMapPreviewPointUi(): TrackMapPreviewPointUi = TrackMapPreviewPointUi(x = x, y = y)

fun TrackMapBounds.toTrackMapPreviewBoundsUi(): TrackMapPreviewBoundsUi = TrackMapPreviewBoundsUi(
    minX = minX,
    minY = minY,
    maxX = maxX,
    maxY = maxY,
)
