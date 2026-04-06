package com.analyzer.session.analysis.presentation.components.map.resolver

import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarkerResolverInput
import com.analyzer.session.analysis.presentation.model.CornerScoreUi

internal data class CornerMarkerPlacementContext(
    val geometry: CornerMarkerGeometry,
    val corner: CornerScoreUi,
    val occupiedPositions: List<Offset>,
    val input: TrackCornerMarkerResolverInput,
)
