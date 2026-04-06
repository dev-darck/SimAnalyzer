package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi

internal data class TrackCornerMarkerResolverInput(
    val trackCenter: Offset,
    val canvasSize: IntSize,
    val palette: TrackDiagnosticPalette,
    val placement: TrackCornerMarkerPlacement,
    val trackLeftEdge: List<SessionAnalysisFractionPointUi> = emptyList(),
    val trackRightEdge: List<SessionAnalysisFractionPointUi> = emptyList(),
)
