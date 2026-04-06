package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

internal data class MatchedCornerWindow(
    val cornerNumber: Int,
    val window: CornerWindow,
    val reference: SessionAnalysisCornerReference? = null,
)
