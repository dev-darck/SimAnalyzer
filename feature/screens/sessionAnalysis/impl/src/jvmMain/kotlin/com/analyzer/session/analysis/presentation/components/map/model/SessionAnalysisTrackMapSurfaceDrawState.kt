package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi

internal data class SessionAnalysisTrackMapSurfaceDrawState(
    val trackSurfaceBasePath: Path,
    val trackSurfacePath: Path?,
    val clipTelemetryToSurface: Boolean,
    val showSurfaceEdges: Boolean,
    val trackSurfaceColor: Color,
    val leftEdgePath: Path,
    val rightEdgePath: Path,
    val trackEdgeColor: Color,
    val centerLineInView: List<SessionAnalysisFractionPointUi>,
    val surfaceStrokePx: Float,
    val strokeScale: Float,
)
