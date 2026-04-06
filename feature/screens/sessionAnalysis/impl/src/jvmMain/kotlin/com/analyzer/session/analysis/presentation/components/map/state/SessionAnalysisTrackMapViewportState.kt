package com.analyzer.session.analysis.presentation.components.map.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapCameraState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPointerState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorBoundary
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorMarker
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class SessionAnalysisTrackMapViewportState(
    val surfaceStrokePx: Float,
    val centerLineInView: ImmutableList<SessionAnalysisFractionPointUi>,
    val leftEdgeInView: ImmutableList<SessionAnalysisFractionPointUi>,
    val rightEdgeInView: ImmutableList<SessionAnalysisFractionPointUi>,
    val idealLineInView: ImmutableList<SessionAnalysisFractionPointUi>,
    val idealLineBreakIndices: Set<Int> = emptySet(),
    val selectedTraceInView: ImmutableList<SessionAnalysisFractionPointUi>,
    val selectedTraceBreakIndices: Set<Int> = emptySet(),
    val selectedTrailTraceInView: ImmutableList<SessionAnalysisFractionPointUi>,
    val selectedTrailTraceBreakIndices: Set<Int> = emptySet(),
    val referenceTraceInView: ImmutableList<SessionAnalysisFractionPointUi>,
    val referenceTraceBreakIndices: Set<Int> = emptySet(),
    val sectorBoundariesInView: ImmutableList<SessionAnalysisTrackSectorBoundary>,
    val sectorMarkersInView: ImmutableList<SessionAnalysisTrackSectorMarker>,
    val activeMarkerInView: SessionAnalysisFractionPointUi?,
    val displayMarkerDirection: Offset?,
    val overlayDimAlpha: Float,
    val overlayMarkerScale: Float,
    val cameraState: SessionAnalysisTrackMapCameraState,
    val pointerState: SessionAnalysisTrackMapPointerState,
)
