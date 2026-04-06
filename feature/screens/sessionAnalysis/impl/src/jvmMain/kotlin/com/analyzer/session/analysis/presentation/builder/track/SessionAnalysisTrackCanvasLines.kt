package com.analyzer.session.analysis.presentation.builder.track

import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableList

/**
 * Prepares center, edge, and overlay lines so the canvas can render from one flattened geometry bundle.
 */
internal data class SessionAnalysisTrackCanvasLines(
    val centerLine: ImmutableList<SessionAnalysisFractionPointUi>,
    val trackLeftEdge: ImmutableList<SessionAnalysisFractionPointUi>,
    val trackRightEdge: ImmutableList<SessionAnalysisFractionPointUi>,
    val idealLine: ImmutableList<SessionAnalysisFractionPointUi>,
    val pitLine: ImmutableList<SessionAnalysisFractionPointUi>,
    val selectedTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    val referenceTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    val interactionTrace: ImmutableList<SessionAnalysisFractionPointUi>,
)
