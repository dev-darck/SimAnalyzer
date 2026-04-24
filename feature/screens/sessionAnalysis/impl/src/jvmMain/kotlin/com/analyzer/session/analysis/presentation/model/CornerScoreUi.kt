package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

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
    val source: SessionAnalysisDiagnosisSourceUi = SessionAnalysisDiagnosisSourceUi.DrivingStyle,
    val category: SessionAnalysisHighlightCategoryUi? = null,
)
