package com.analyzer.session.analysis.presentation.builder.track

import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import kotlinx.collections.immutable.ImmutableList

internal data class SessionAnalysisTrackCanvasStateInput(
    val trackMap: com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackMapUi?,
    val authoredTrackMap: SessionAnalysisTrackMap?,
    val sourceTrackMap: SessionAnalysisTrackMap?,
    val displayTrackMap: SessionAnalysisTrackMap?,
    val visibleSamplesCore: List<SessionAnalysisSample>,
    val referenceSamplesCore: List<SessionAnalysisSample>,
    val sectors: ImmutableList<com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi>,
    val highlights: ImmutableList<com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi>,
    val diagnosticSummary: com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi?,
    val cornerZones: List<SessionAnalysisCornerZone> = emptyList(),
)
