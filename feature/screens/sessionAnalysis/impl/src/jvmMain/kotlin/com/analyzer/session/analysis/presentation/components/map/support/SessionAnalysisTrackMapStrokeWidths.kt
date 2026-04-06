package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisTrackMapStrokeWidths(
    val sectorBoundaryCasing: Float,
    val sectorBoundaryCore: Float,
    val referenceCasing: Float,
    val referenceCore: Float,
    val idealCasing: Float,
    val idealCore: Float,
    val selectedBase: Float,
    val selectedTrailCasing: Float,
    val selectedTrailCore: Float,
    val markerRingRadius: Float,
    val markerRingStroke: Float,
    val markerInnerRadius: Float,
    val markerDirectionLength: Float,
)
