package com.project.analyzer.telemetry.analysis.api.model.report.setup

import com.project.analyzer.telemetry.analysis.api.model.report.common.Priority

public data class SetupRecommendation(
    val category: SetupCategory = SetupCategory.BRAKE_BIAS,
    val currentValue: String? = null,
    val suggestedChange: String = "",
    val reason: String = "",
    val expectedBenefit: String = "",
    val confidence: Float = 0f,
    val affectedCorners: List<Int> = emptyList(),
    val priority: Priority = Priority.MEDIUM,
)
