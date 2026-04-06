package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource

internal data class SessionAnalysisCornerSetupInsight(
    val diagnosisSource: SessionAnalysisDiagnosisSource,
    val recommendation: String,
)
