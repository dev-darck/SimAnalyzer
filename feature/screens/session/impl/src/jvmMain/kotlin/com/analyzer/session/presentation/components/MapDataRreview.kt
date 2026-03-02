package com.analyzer.session.presentation.components

import com.project.analyzer.ui.components.TrackMapBounds
import com.project.analyzer.ui.components.TrackMapData
import com.project.analyzer.ui.components.TrackMapPoint

internal fun previewTrackMapData(): TrackMapData = TrackMapData(
    points = listOf(
        TrackMapPoint(x = 0.0f, y = 9.5f),
        TrackMapPoint(x = 2.0f, y = 8.1f),
        TrackMapPoint(x = 5.0f, y = 7.4f),
        TrackMapPoint(x = 8.6f, y = 7.8f),
        TrackMapPoint(x = 11.8f, y = 9.4f),
        TrackMapPoint(x = 13.6f, y = 12.0f),
        TrackMapPoint(x = 13.8f, y = 15.2f),
        TrackMapPoint(x = 12.2f, y = 17.8f),
        TrackMapPoint(x = 9.2f, y = 18.9f),
        TrackMapPoint(x = 6.0f, y = 18.2f),
        TrackMapPoint(x = 3.5f, y = 16.3f),
        TrackMapPoint(x = 1.4f, y = 13.5f),
        TrackMapPoint(x = 0.0f, y = 9.5f),
    ),
    bounds = TrackMapBounds(
        minX = 0.0f,
        minY = 7.4f,
        maxX = 13.8f,
        maxY = 18.9f,
    ),
)
