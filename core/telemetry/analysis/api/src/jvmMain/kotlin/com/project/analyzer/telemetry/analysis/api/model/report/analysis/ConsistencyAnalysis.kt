package com.project.analyzer.telemetry.analysis.api.model.report.analysis

public data class ConsistencyAnalysis(
    val lapTimes: List<Long> = emptyList(),
    val avgLapTime: Long = 0L,
    val stdDev: Long = 0L,
    val consistencyScore: Int = 0,
    val cornerConsistency: Map<Int, CornerConsistency> = emptyMap(),
    val sectorConsistency: Map<Int, SectorConsistency> = emptyMap(),
    val mostConsistentSegment: Int = 0,
    val leastConsistentSegment: Int = 0,
    val fatigueDetected: Boolean = false,
    val fatigueOnsetLap: Int? = null,
    val recommendations: List<String> = emptyList(),
)
