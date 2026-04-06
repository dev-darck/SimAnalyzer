package com.project.analyzer.telemetry.analysis.api.model.report.corner

public data class CornerThresholds(
    val highSpeedCornerMin: Float = 0f,
    val mediumSpeedCornerMin: Float = 0f,
    val optimalTrailBrakeRange: ClosedFloatingPointRange<Float> = 0f..0f,
    val maxSlipAngle: Float = 0f,
    val understeerWarningThreshold: Float = 0f,
    val oversteerWarningThreshold: Float = 0f,
    val wheelSpinThreshold: Float = 0f,
    val throttleSmoothnessTarget: Float = 0f,
)
