package com.analyzer.session.analysis.presentation.components.map.model

import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableList

internal data class SessionAnalysisTrackMapInView(
    val centerLine: ImmutableList<SessionAnalysisFractionPointUi>,
    val leftEdge: ImmutableList<SessionAnalysisFractionPointUi>,
    val rightEdge: ImmutableList<SessionAnalysisFractionPointUi>,
    val idealLine: ImmutableList<SessionAnalysisFractionPointUi>,
    val idealLineBreakIndices: Set<Int>,
    val selectedTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    val selectedTraceBreakIndices: Set<Int>,
    val selectedTrailTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    val referenceTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    val referenceTraceBreakIndices: Set<Int>,
    val sectorBoundaries: ImmutableList<SessionAnalysisTrackSectorBoundary>,
)
