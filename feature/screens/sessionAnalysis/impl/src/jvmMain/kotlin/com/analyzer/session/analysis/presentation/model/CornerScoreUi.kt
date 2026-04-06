package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory

@Immutable
internal data class CornerScoreUi(
    val cornerNumber: Int,
    val score: Int,
    val trackPosition: Float,
    val markerTrackPosition: Float = trackPosition,
    val mainIssue: String? = null,
    val detail: String = "",
    val timeVsReferenceMs: Int = 0,
    val recommendation: String = "",
    val source: SessionAnalysisDiagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
    val category: SessionAnalysisHighlightCategory? = null,
)
