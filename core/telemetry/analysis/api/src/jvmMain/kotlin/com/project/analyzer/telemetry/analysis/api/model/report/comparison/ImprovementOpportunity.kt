package com.project.analyzer.telemetry.analysis.api.model.report.comparison

import com.project.analyzer.telemetry.analysis.api.model.report.common.Difficulty

public data class ImprovementOpportunity(
    val segment: String = "",
    val currentTime: Long = 0L,
    val referenceTime: Long = 0L,
    val potentialGain: Long = 0L,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val action: String = "",
)
