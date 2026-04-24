package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class DiagnosticIssueUi(
    val title: String,
    val description: String,
    val recommendation: String,
    val priority: Int,
    val source: SessionAnalysisDiagnosisSourceUi,
    val potentialTimeGainMs: Int,
    val category: SessionAnalysisHighlightCategoryUi? = null,
    val cornerNumber: Int? = null,
    val trackPosition: Float? = null,
)
