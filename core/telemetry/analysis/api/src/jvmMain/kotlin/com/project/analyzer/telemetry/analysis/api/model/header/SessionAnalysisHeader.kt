package com.project.analyzer.telemetry.analysis.api.model.header
public data class SessionAnalysisHeader(
    val gameId: String = "",
    val sessionTypeLabel: String = "",
    val carLabel: String = "",
    val trackLabel: String = "",
    val startedAtMs: Long = 0L,
    val airTempC: Float? = null,
    val trackTempC: Float? = null,
)
