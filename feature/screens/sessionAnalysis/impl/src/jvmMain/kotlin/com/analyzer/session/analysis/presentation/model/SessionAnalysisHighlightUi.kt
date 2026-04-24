package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisHighlightUi(
    val id: String = "",
    val category: SessionAnalysisHighlightCategoryUi,
    val severity: SessionAnalysisHighlightSeverityUi,
    val lapNumber: Int,
    val title: String,
    val description: String,
    val trackPosition: Float? = null,
    val deltaMs: Int? = null,
    val diagnosisSource: SessionAnalysisDiagnosisSourceUi = SessionAnalysisDiagnosisSourceUi.DrivingStyle,
    val recommendation: String = "",
    val cornerNumber: Int? = null,
    val score: Int? = null,
    val priority: Int = 1,
    val affectedLaps: ImmutableList<Int> = persistentListOf(),
    val relatedHighlightIds: ImmutableList<String> = persistentListOf(),
)
