package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey

internal data class SetupDiagnosticInput(
    val corners: List<SessionAnalysisCornerAnalysis>,
    val samples: List<SessionAnalysisSample>,
    val tyreProfile: SessionAnalysisTyreProfile?,
)

internal data class SetupDiagnosticStageResult(
    val diagnostics: List<SessionAnalysisSetupDiagnostic> = emptyList(),
    val cornerInsights: Map<SessionAnalysisCornerKey, SessionAnalysisCornerSetupInsight> = emptyMap(),
)
