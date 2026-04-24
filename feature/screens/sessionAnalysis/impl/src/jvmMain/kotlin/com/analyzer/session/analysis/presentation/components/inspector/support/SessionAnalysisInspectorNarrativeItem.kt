package com.analyzer.session.analysis.presentation.components.inspector.support

import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosisSourceUi

internal data class SessionAnalysisInspectorNarrativeItem(
    val title: String,
    val description: String,
    val recommendation: String = "",
    val lookAt: String = "",
    val source: SessionAnalysisDiagnosisSourceUi = SessionAnalysisDiagnosisSourceUi.DrivingStyle,
)
