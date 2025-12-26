package com.project.analyzer.calibration.data.model

data class Pose2D(
    val pos: Vec2,
    val forward: Vec2,
)

data class CalibrationSample(
    val pose: Pose2D?,
    val speedKmh: Float,
    val headingRad: Float,
    val timestampNs: Long = 0
)
