package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisHighlightUi(
    val id: String = "",
    val category: SessionAnalysisHighlightCategory,
    val severity: SessionAnalysisHighlightSeverity,
    val lapNumber: Int,
    val title: String,
    val description: String,
    val trackPosition: Float? = null,
    val deltaMs: Int? = null,
    val diagnosisSource: SessionAnalysisDiagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
    val recommendation: String = "",
    val cornerNumber: Int? = null,
    val score: Int? = null,
    val priority: Int = 1,
    val affectedLaps: ImmutableList<Int> = persistentListOf(),
    val relatedHighlightIds: ImmutableList<String> = persistentListOf(),
)
