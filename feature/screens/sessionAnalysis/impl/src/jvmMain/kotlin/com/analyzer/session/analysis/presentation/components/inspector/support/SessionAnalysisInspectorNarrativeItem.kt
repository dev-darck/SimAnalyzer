package com.analyzer.session.analysis.presentation.components.inspector.support

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource

internal data class SessionAnalysisInspectorNarrativeItem(
    val title: String,
    val description: String,
    val recommendation: String = "",
    val lookAt: String = "",
    val source: SessionAnalysisDiagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
)
