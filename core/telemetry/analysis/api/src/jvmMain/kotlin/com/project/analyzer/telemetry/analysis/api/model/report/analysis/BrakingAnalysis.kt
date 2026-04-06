package com.project.analyzer.telemetry.analysis.api.model.report.analysis

import com.project.analyzer.telemetry.analysis.api.model.report.common.IssueSeverity
import com.project.analyzer.telemetry.analysis.api.model.report.common.WheelPosition

public data class BrakingAnalysis(
    val zonePosition: Float = 0f,
    val entrySpeed: Float = 0f,
    val cornerSpeed: Float = 0f,
    val speedReduction: Float = 0f,
    val brakePoint: Float = 0f,
    val referenceBrakePoint: Float = 0f,
    val brakePointDelta: Float = 0f,
    val peakBrakePressure: Float = 0f,
    val avgBrakePressure: Float = 0f,
    val brakeSmoothness: Float = 1f,
    val brakeTrailRatio: Float = 0f,
    val lockupDetected: Boolean = false,
    val lockupWheel: WheelPosition? = null,
    val lockupDuration: Long = 0L,
    val absActivation: Boolean = false,
    val absActivationDuration: Long = 0L,
    val timeDelta: Long = 0L,
    val distanceDelta: Float = 0f,
    val recommendation: String? = null,
    val severity: IssueSeverity = IssueSeverity.NEUTRAL,
)
