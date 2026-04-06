package com.project.analyzer.telemetry.analysis.api.model.report.comparison

import com.project.analyzer.telemetry.analysis.api.model.report.segment.SegmentIssue

public data class SegmentComparison(
    val segmentId: Int = 0,
    val segmentName: String = "",
    val currentTime: Long = 0L,
    val referenceTime: Long = 0L,
    val delta: Long = 0L,
    val dominantIssue: SegmentIssue? = null,
)
