package com.analyzer.trackmap.presentation.mapper

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi

fun TrackMapLibraryItem.toTrackMapPreviewUi(): TrackMapPreviewUi = TrackMapPreviewUi(
    points = points,
    leftWidthsMeters = leftWidthsMeters,
    rightWidthsMeters = rightWidthsMeters,
    pointCount = points.size,
    totalDistanceMeters = distanceMeters,
    bounds = bounds,
    pitPoints = pitPoints,
    pitEntryPoint = pitEntryPoint,
    pitExitPoint = pitExitPoint,
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
