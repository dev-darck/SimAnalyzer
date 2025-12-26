package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.telemetry.ac.api.model.math.Vec2

data class CarPose(
    val position: Vec2,
    val velocityDir: Vec2,
    val headingDir: Vec2,
    val speedKmh: Float,
    val isMovingForward: Boolean,
)
