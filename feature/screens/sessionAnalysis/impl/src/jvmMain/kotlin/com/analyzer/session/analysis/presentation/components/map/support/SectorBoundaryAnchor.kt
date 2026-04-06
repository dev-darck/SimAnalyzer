package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.ui.geometry.Offset

internal data class SectorBoundaryAnchor(
    val centerOffset: Offset,
    val direction: Offset?,
    val useSegmentEdgeDistance: Boolean,
)
