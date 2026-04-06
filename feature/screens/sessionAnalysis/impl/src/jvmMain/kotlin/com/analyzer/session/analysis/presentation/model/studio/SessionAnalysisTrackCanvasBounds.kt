package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisTrackCanvasBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
)
