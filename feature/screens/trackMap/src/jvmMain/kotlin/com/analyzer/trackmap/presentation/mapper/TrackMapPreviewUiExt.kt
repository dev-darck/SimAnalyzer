package com.analyzer.trackmap.presentation.mapper

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi
import com.analyzer.trackmap.presentation.model.toTrackMapPreviewBoundsUi
import com.analyzer.trackmap.presentation.model.toTrackMapPreviewPointUi
import kotlinx.collections.immutable.toImmutableList

fun TrackMapLibraryItem.toTrackMapPreviewUi(): TrackMapPreviewUi = TrackMapPreviewUi(
    points = points.map { it.toTrackMapPreviewPointUi() }.toImmutableList(),
    pointCount = points.size,
    totalDistanceMeters = distanceMeters,
    bounds = bounds?.toTrackMapPreviewBoundsUi(),
    pitPoints = pitPoints.map { it.toTrackMapPreviewPointUi() }.toImmutableList(),
    pitEntryPoint = pitEntryPoint?.toTrackMapPreviewPointUi(),
    pitExitPoint = pitExitPoint?.toTrackMapPreviewPointUi(),
    averageTrackWidthMeters = averageTrackWidthMeters,
)
