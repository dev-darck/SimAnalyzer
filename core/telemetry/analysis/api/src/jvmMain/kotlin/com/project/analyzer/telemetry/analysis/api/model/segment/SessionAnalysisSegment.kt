package com.project.analyzer.telemetry.analysis.api.model.segment
public data class SessionAnalysisSegment(
    val segmentId: Long,
    val sessionTypeLabel: String = "",
    val sessionLabel: String = "",
    val carLabel: String = "",
    val trackLabel: String = "",
    val startedAtMs: Long = 0L,
    val endedAtMs: Long? = null,
)
