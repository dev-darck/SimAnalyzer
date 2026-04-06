package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity

internal data class SessionAnalysisHighlightDraft(
    val category: SessionAnalysisHighlightCategory,
    val severity: SessionAnalysisHighlightSeverity,
    val segmentId: Long,
    val lapNumber: Int,
    val sampleIndexInLap: Int,
    val trackPosition: Float?,
    val title: String,
    val description: String,
    val deltaMs: Int? = null,
    val diagnosisSource: SessionAnalysisDiagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
    val recommendation: String = "",
    val cornerNumber: Int? = null,
    val score: Int? = null,
    val affectedLaps: List<Int> = emptyList(),
)
