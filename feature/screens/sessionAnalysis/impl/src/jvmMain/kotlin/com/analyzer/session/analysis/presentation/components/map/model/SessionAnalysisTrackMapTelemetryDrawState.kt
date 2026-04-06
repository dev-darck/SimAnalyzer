package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.analyzer.session.analysis.presentation.components.map.support.SessionAnalysisTrackMapStrokeWidths
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi

internal data class SessionAnalysisTrackMapTelemetryDrawState(
    val sectorBoundariesInView: List<SessionAnalysisTrackSectorBoundary>,
    val referenceTraceInView: List<SessionAnalysisFractionPointUi>,
    val referenceLinePath: Path,
    val idealLineInView: List<SessionAnalysisFractionPointUi>,
    val idealLinePath: Path,
    val selectedTraceInView: List<SessionAnalysisFractionPointUi>,
    val selectedBasePath: Path,
    val selectedTrailTraceInView: List<SessionAnalysisFractionPointUi>,
    val selectedTrailPath: Path,
    val activeMarkerInView: SessionAnalysisFractionPointUi?,
    val displayMarkerDirection: Offset?,
    val strokeWidths: SessionAnalysisTrackMapStrokeWidths,
)
