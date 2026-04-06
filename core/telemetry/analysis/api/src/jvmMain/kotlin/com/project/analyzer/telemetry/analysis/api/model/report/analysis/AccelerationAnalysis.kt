package com.project.analyzer.telemetry.analysis.api.model.report.analysis

import com.project.analyzer.telemetry.analysis.api.model.report.common.IssueSeverity
import com.project.analyzer.telemetry.analysis.api.model.report.common.WheelPosition

public data class AccelerationAnalysis(
    val zonePosition: Float = 0f,
    val exitSpeed: Float = 0f,
    val referenceExitSpeed: Float = 0f,
    val throttlePickupPoint: Float = 0f,
    val throttlePickupSmoothness: Float = 1f,
    val avgThrottle: Float = 0f,
    val timeToFullThrottle: Long = 0L,
    val wheelSpinDetected: Boolean = false,
    val wheelSpinWheels: List<WheelPosition> = emptyList(),
    val wheelSpinSeverity: Float = 0f,
    val wheelSpinDuration: Long = 0L,
    val tractionControlActive: Boolean = false,
    val tractionControlInterventions: Int = 0,
    val understeerOnExit: Boolean = false,
    val oversteerOnExit: Boolean = false,
    val timeDelta: Long = 0L,
    val speedDelta: Float = 0f,
    val recommendation: String? = null,
    val severity: IssueSeverity = IssueSeverity.NEUTRAL,
)
