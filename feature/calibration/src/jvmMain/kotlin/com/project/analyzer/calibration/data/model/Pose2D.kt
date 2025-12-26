package com.project.analyzer.calibration.data.model

import com.project.analyzer.telemetry.ac.api.model.math.Vec2

data class Pose2D(
    val pos: Vec2,
    val forward: Vec2,
)

data class WheelDebug(
    val fl: Vec2?,
    val fr: Vec2?,
    val rl: Vec2?,
    val rr: Vec2?,
)

data class CalibrationSample(
    val pose: Pose2D?,
    val speedKmh: Float,
    val headingRad: Float,
    val timestampNs: Long = 0L,
    val wheels: WheelDebug? = null,
    val axleForward: Vec2? = null
)
