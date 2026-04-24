package com.analyzer.session.analysis.presentation.components.inspector.card

import com.analyzer.session.analysis.presentation.components.inspector.support.SessionAnalysisSetupSystem
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosisSourceUi

internal data class SessionAnalysisSetupAdviceItem(
    val title: String,
    val description: String,
    val recommendation: String,
    val lookAt: String = "",
    val source: SessionAnalysisDiagnosisSourceUi,
    val trackPosition: Float?,
    val system: SessionAnalysisSetupSystem,
)
