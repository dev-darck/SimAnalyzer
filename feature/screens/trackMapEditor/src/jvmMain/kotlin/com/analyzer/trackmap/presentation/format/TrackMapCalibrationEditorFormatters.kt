package com.analyzer.trackmap.presentation.format

import androidx.compose.ui.graphics.Color
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorMarker

internal fun formatMeters(value: Float): String = "%03.0f m".format(value)

internal fun TrackMapCalibrationEditorMarker.color(): Color = Color(colorHex)

internal fun List<TrackMapCalibrationEditorMarker>.previousOf(gateId: String): TrackMapCalibrationEditorMarker? {
    if (isEmpty()) return null
    val index = indexOfFirst { it.gateId == gateId }
    if (index == -1) return null
    val previousIndex = if (index == 0) lastIndex else index - 1
    return getOrNull(previousIndex)
}
