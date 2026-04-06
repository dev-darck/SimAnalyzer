package com.project.analyzer.telemetry.analysis.api.model.report.analysis

import com.project.analyzer.telemetry.analysis.api.model.report.common.IssueSeverity

public data class SteeringAnalysis(
    val segmentPosition: Float = 0f,
    val avgSteeringAngle: Float = 0f,
    val maxSteeringAngle: Float = 0f,
    val steeringSmoothness: Float = 1f,
    val lineDeviation: Float = 0f,
    val referenceLineDeviation: Float = 0f,
    val inputCount: Int = 0,
    val sawtoothDetected: Boolean = false,
    val understeerDetected: Boolean = false,
    val understeerSeverity: Float = 0f,
    val oversteerDetected: Boolean = false,
    val oversteerSeverity: Float = 0f,
    val recommendation: String? = null,
    val severity: IssueSeverity = IssueSeverity.NEUTRAL,
)
