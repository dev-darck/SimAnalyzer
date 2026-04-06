package com.analyzer.session.analysis.presentation.components.inspector.card

import com.analyzer.session.analysis.presentation.components.inspector.support.SessionAnalysisSetupSystem
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource

internal data class SessionAnalysisSetupAdviceItem(
    val title: String,
    val description: String,
    val recommendation: String,
    val lookAt: String = "",
    val source: SessionAnalysisDiagnosisSource,
    val trackPosition: Float?,
    val system: SessionAnalysisSetupSystem,
)
