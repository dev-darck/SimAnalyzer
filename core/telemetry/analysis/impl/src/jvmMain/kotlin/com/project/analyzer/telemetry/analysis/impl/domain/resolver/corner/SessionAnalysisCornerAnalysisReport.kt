package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

internal data class SessionAnalysisCornerAnalysisReport(
    val corners: List<SessionAnalysisCornerAnalysis> = emptyList(),
    val referenceCornersBySegmentId: Map<Long, List<SessionAnalysisCornerReference>> = emptyMap(),
)
