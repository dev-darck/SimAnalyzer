package com.analyzer.trackmap.presentation.mapper

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.domain.model.mapKey
import com.analyzer.trackmap.presentation.mapper.toTrackMapPreviewUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi

internal fun TrackMapLibraryItem.toTrackMapLibraryCardUi(): TrackMapLibraryCardUi {
    val preview = toTrackMapPreviewUi()
    return TrackMapLibraryCardUi(
        mapKey = mapKey(),
        gameId = map.gameId,
        trackId = map.trackId,
        trackName = map.trackName,
        layoutId = map.layoutId,
        createdAtEpochMs = map.createdAtEpochMs,
        pointCount = points.size,
        points = points,
        distanceMeters = distanceMeters,
        pitPointCount = pitPoints.size,
        preview = preview,
        averageTrackWidthMeters = preview.averageTrackWidthMeters,
    )
}
