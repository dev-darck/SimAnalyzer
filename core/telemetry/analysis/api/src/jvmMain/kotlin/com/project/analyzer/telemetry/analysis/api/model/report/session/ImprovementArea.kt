package com.project.analyzer.telemetry.analysis.api.model.report.session

public data class ImprovementArea(
    val title: String = "",
    val description: String = "",
    val potentialGain: Long = 0L,
    val confidence: Float = 0f,
)
