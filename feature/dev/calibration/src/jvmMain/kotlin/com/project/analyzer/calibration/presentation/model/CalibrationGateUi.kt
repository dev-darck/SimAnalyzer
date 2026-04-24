package com.project.analyzer.calibration.presentation.model

import androidx.compose.runtime.Immutable
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate

@Immutable
data class CalibrationGateUi(
    val center: Vec2,
    val forward: Vec2,
    val normal: Vec2,
    val halfWidthMeters: Float,
)

internal fun CalibrationGateUi.toDomain(): Gate = Gate.create(
    center = center,
    forward = forward,
    normal = normal,
    halfWidthMeters = halfWidthMeters,
)

internal fun Gate.toUi(): CalibrationGateUi = CalibrationGateUi(
    center = centerV2(),
    forward = forwardV2(),
    normal = normalV2(),
    halfWidthMeters = halfWidthMeters,
)
