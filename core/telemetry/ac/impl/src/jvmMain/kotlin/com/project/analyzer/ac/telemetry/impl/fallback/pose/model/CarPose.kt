package com.project.analyzer.ac.telemetry.impl.fallback.pose.model

import com.project.analyzer.math.Vec2

data class CarPose(
    val position: Vec2,
    val velocityDir: Vec2,
    val headingDir: Vec2,
    val speedKmh: Float,
    val isMovingForward: Boolean,
)
