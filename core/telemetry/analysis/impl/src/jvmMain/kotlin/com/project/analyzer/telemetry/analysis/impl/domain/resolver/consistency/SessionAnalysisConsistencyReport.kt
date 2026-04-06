package com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency

internal data class SessionAnalysisConsistencyReport(
    val overallScore: Int = 100,
    val corners: List<SessionAnalysisCornerConsistency> = emptyList(),
)
