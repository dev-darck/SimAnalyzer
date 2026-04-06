package com.project.analyzer.telemetry.analysis.api.model.report.analysis

public data class CornerConsistency(
    val cornerNumber: Int = 0,
    val avgSpeed: Float = 0f,
    val stdDev: Float = 0f,
    val consistencyScore: Int = 0,
    val trend: Trend = Trend.STABLE,
)
