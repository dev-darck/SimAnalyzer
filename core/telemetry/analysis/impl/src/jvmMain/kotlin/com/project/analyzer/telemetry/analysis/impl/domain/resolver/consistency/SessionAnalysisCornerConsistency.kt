package com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency

internal data class SessionAnalysisCornerConsistency(
    val segmentId: Long,
    val cornerNumber: Int,
    val trackPosition: Float,
    val score: Int,
    val brakePointStdPct: Float? = null,
    val apexSpeedStdKmh: Float? = null,
    val throttlePickupStdPct: Float? = null,
    val exitSpeedStdKmh: Float? = null,
    val lineVarianceMeters: Float? = null,
    val affectedLaps: List<Int> = emptyList(),
)
