package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetrySeriesTone

internal data class SessionAnalysisChartSeriesSeed(
    val label: String,
    val tone: SessionAnalysisTelemetrySeriesTone,
    val dashed: Boolean,
    val smoothingWindowRadius: Int,
    val rawPoints: List<SessionAnalysisRawChartPoint>,
)
