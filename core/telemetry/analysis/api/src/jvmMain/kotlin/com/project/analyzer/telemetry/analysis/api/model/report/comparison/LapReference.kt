package com.project.analyzer.telemetry.analysis.api.model.report.comparison

public data class LapReference(
    val lapNumber: Int = 0,
    val lapTime: Long = 0L,
    val valid: Boolean = false,
    val label: String = "",
)
