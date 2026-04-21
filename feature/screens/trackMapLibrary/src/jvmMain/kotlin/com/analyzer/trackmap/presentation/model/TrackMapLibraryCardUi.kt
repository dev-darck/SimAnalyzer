package com.analyzer.trackmap.presentation.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TrackMapLibraryHeaderUi(val title: String, val subtitle: String, val layoutLabel: String? = null)

@Immutable
internal data class TrackMapLibraryStatsUi(
    val pointCount: Int,
    val distanceMeters: Float,
    val averageTrackWidthMeters: Float,
    val pitPointCount: Int,
    val createdAtLabel: String,
)

@Immutable
internal data class TrackMapLibraryPointsPreviewUi(
    val points: ImmutableList<TrackMapPreviewPointUi> = persistentListOf(),
    val hiddenCount: Int = 0,
)

@Immutable
internal data class TrackMapLibraryCardUi(
    val mapKey: String,
    val gameId: String,
    val trackId: String,
    val layoutId: String?,
    val header: TrackMapLibraryHeaderUi,
    val stats: TrackMapLibraryStatsUi,
    val preview: TrackMapPreviewUi,
    val pointsPreview: TrackMapLibraryPointsPreviewUi,
)
