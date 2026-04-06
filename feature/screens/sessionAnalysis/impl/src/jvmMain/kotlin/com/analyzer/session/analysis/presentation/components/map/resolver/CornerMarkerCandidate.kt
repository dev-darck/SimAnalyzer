package com.analyzer.session.analysis.presentation.components.map.resolver

import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarkerSide

internal data class CornerMarkerCandidate(
    val position: Offset,
    val side: TrackCornerMarkerSide,
    val score: Float,
    val valid: Boolean,
)
