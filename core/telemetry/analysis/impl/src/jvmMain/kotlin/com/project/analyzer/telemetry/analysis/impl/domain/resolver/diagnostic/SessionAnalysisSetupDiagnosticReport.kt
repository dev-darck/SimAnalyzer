package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey

internal data class SessionAnalysisSetupDiagnosticReport(
    val diagnostics: List<SessionAnalysisSetupDiagnostic> = emptyList(),
    val cornerInsights: Map<SessionAnalysisCornerKey, SessionAnalysisCornerSetupInsight> = emptyMap(),
)
