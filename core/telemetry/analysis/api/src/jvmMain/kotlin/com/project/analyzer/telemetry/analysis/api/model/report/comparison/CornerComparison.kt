package com.project.analyzer.telemetry.analysis.api.model.report.comparison

import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerIssue

public data class CornerComparison(
    val cornerNumber: Int = 0,
    val currentTime: Long = 0L,
    val referenceTime: Long = 0L,
    val delta: Long = 0L,
    val issue: CornerIssue? = null,
)
