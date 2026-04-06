package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetrySeriesTone

internal data class SessionAnalysisChartSeriesSpec(
    val label: String,
    val tone: SessionAnalysisTelemetrySeriesTone,
    val dashed: Boolean,
    val smoothingWindowRadius: Int = 0,
    val selector: (SessionAnalysisComparisonPointUi) -> Float?,
)
