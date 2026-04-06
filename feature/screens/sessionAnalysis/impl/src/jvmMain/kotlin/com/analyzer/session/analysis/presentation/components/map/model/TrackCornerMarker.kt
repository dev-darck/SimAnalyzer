package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.analyzer.session.analysis.presentation.model.CornerScoreUi

@Immutable
internal data class TrackCornerMarker(
    val corner: CornerScoreUi,
    val position: Offset,
    val accent: Color,
    val turnDirection: TrackCornerTurnDirection,
    val placementSide: TrackCornerMarkerSide,
)
