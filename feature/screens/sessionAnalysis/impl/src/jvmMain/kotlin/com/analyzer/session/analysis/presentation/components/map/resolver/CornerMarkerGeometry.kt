package com.analyzer.session.analysis.presentation.components.map.resolver

import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarkerSide
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerTurnDirection

internal data class CornerMarkerGeometry(
    val anchorOffset: Offset,
    val tangentDirection: Offset,
    val outerNormal: Offset,
    val innerNormal: Offset,
    val turnDirection: TrackCornerTurnDirection,
) {

    fun normalFor(side: TrackCornerMarkerSide): Offset = when (side) {
        TrackCornerMarkerSide.Inner -> innerNormal
        TrackCornerMarkerSide.Outer -> outerNormal
    }
}
