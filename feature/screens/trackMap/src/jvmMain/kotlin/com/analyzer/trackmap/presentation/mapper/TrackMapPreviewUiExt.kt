package com.analyzer.trackmap.presentation.mapper

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi
import com.analyzer.trackmap.presentation.model.toTrackMapPreviewBoundsUi
import com.analyzer.trackmap.presentation.model.toTrackMapPreviewPointUi
import kotlinx.collections.immutable.toImmutableList

fun TrackMapLibraryItem.toTrackMapPreviewUi(): TrackMapPreviewUi = TrackMapPreviewUi(
    points = points.map { it.toTrackMapPreviewPointUi() }.toImmutableList(),
    leftWidthsMeters = leftWidthsMeters.toImmutableList(),
    rightWidthsMeters = rightWidthsMeters.toImmutableList(),
    pointCount = points.size,
    totalDistanceMeters = distanceMeters,
    bounds = bounds?.toTrackMapPreviewBoundsUi(),
    pitPoints = pitPoints.map { it.toTrackMapPreviewPointUi() }.toImmutableList(),
    pitEntryPoint = pitEntryPoint?.toTrackMapPreviewPointUi(),
    pitExitPoint = pitExitPoint?.toTrackMapPreviewPointUi(),
    averageTrackWidthMeters = averageTrackWidthMeters(),
)

private fun TrackMapLibraryItem.averageTrackWidthMeters(): Float {
    if (leftWidthsMeters.isEmpty() || rightWidthsMeters.isEmpty()) return 0f
    val size = minOf(leftWidthsMeters.size, rightWidthsMeters.size)
    if (size == 0) return 0f

    var sum = 0f
    for (index in 0 until size) {
        sum += leftWidthsMeters[index] + rightWidthsMeters[index]
    }
    return sum / size
}
