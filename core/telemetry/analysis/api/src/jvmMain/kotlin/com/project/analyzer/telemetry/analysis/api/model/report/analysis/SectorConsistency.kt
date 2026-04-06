package com.project.analyzer.telemetry.analysis.api.model.report.analysis

public data class SectorConsistency(
    val sectorNumber: Int = 0,
    val avgDelta: Long = 0L,
    val stdDev: Long = 0L,
    val consistencyScore: Int = 0,
    val trend: Trend = Trend.STABLE,
)
