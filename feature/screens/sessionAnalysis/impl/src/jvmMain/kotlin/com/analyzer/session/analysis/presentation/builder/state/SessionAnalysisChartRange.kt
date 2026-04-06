package com.analyzer.session.analysis.presentation.builder.state

internal data class SessionAnalysisChartRange(
    val minValue: Float,
    val maxValue: Float,
    val zeroBaseline: Float? = null,
)
