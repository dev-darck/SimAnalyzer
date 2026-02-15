package com.project.analyzer.ac.telemetry.impl.fallback.detector.model

import com.project.analyzer.math.Vec2

data class GateCrossing(
    val interpolationFactor: Float,
    val isForwardDirection: Boolean,
    val hitPoint: Vec2,
    val outsideByMeters: Float,
)
