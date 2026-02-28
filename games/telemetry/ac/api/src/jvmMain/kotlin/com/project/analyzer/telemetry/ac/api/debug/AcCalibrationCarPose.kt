package com.project.analyzer.telemetry.ac.api.debug

import com.project.analyzer.math.Vec2

public data class AcCalibrationCarPose(
    public val position: Vec2,
    public val velocityDir: Vec2,
    public val headingDir: Vec2,
    public val speedKmh: Float,
    public val isMovingForward: Boolean,
)
