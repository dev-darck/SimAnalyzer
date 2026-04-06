package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory

@Immutable
internal data class DiagnosticIssueUi(
    val title: String,
    val description: String,
    val recommendation: String,
    val priority: Int,
    val source: SessionAnalysisDiagnosisSource,
    val potentialTimeGainMs: Int,
    val category: SessionAnalysisHighlightCategory? = null,
    val cornerNumber: Int? = null,
    val trackPosition: Float? = null,
)
