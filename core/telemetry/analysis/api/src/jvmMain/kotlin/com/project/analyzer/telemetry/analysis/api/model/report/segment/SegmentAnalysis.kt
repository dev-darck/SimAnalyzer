package com.project.analyzer.telemetry.analysis.api.model.report.segment

import com.project.analyzer.telemetry.analysis.api.model.report.common.IssueSeverity
import com.project.analyzer.telemetry.analysis.api.model.report.corner.EnhancedCornerAnalysis

public data class SegmentAnalysis(
    val segmentId: Int = 0,
    val segmentName: String = "",
    val startPosition: Float = 0f,
    val endPosition: Float = 0f,
    val segmentType: SegmentType = SegmentType.STRAIGHT,
    val corners: List<EnhancedCornerAnalysis> = emptyList(),
    val minSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val timeDelta: Long = 0L,
    val isTimeLossSegment: Boolean = false,
    val avgThrottle: Float = 0f,
    val peakThrottle: Float = 0f,
    val avgBrake: Float = 0f,
    val peakBrake: Float = 0f,
    val avgSteeringAngle: Float = 0f,
    val steeringSmoothness: Float = 1f,
    val referenceSegment: SegmentAnalysis? = null,
    val deltaSpeed: Float = 0f,
    val deltaThrottle: Float = 0f,
    val deltaBrake: Float = 0f,
    val issues: List<SegmentIssue> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val severity: IssueSeverity = IssueSeverity.NEUTRAL,
)
