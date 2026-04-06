package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity

internal data class SessionAnalysisSetupDiagnostic(
    val category: SessionAnalysisHighlightCategory,
    val severity: SessionAnalysisHighlightSeverity,
    val title: String,
    val description: String,
    val recommendation: String,
    val diagnosisSource: SessionAnalysisDiagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
    val segmentId: Long = 0L,
    val lapNumber: Int = 0,
    val sampleIndexInLap: Int = 0,
    val trackPosition: Float? = null,
    val cornerNumber: Int? = null,
    val deltaMs: Int? = null,
    val affectedLaps: List<Int> = emptyList(),
)
