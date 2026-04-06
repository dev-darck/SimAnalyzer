package com.project.analyzer.telemetry.analysis.api.model.report.comparison

public data class PerformanceLoss(
    val segment: String = "",
    val delta: Long = 0L,
    val cause: String = "",
    val recommendation: String = "",
)
