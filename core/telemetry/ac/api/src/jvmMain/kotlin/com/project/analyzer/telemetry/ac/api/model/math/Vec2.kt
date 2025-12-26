package com.project.analyzer.telemetry.ac.api.model.math

import kotlin.math.sqrt

public data class Vec2(
    val x: Float,
    val y: Float
) {
    public operator fun minus(o: Vec2): Vec2 = Vec2(x - o.x, y - o.y)
    public fun dot(o: Vec2): Float = x * o.x + y * o.y
    public fun len(): Float = sqrt(x * x + y * y)
}
