package com.analyzer.trackmap.presentation.model

import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorMarker
import com.analyzer.trackmap.presentation.format.formatMeters
import com.analyzer.trackmap.presentation.format.previousOf

internal data class TrackMapMarkerRowUi(
    val marker: TrackMapCalibrationEditorMarker,
    val title: String,
    val startLabel: String,
    val endLabel: String,
)

internal fun buildTrackMapMarkerRows(markers: List<TrackMapCalibrationEditorMarker>): List<TrackMapMarkerRowUi> =
    markers.map { marker ->
        val previousMarker = markers.previousOf(marker.gateId)
        TrackMapMarkerRowUi(
            marker = marker,
            title = marker.title,
            startLabel = formatMeters(previousMarker?.meters ?: marker.meters),
            endLabel = formatMeters(marker.meters),
        )
    }
