package com.project.analyzer.telemetry.ac.api.model.math

import kotlin.math.sqrt

public data class Vec2(
    val x: Float,
    val y: Float
) {

    public operator fun minus(o: Vec2): Vec2 = Vec2(x - o.x, y - o.y)
    public operator fun plus(o: Vec2): Vec2 = Vec2(x + o.x, y + o.y)
    public operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)
    public fun dot(o: Vec2): Float = x * o.x + y * o.y
    public fun len(): Float = sqrt(x * x + y * y)
    public fun normalized(): Vec2 {
        val len = len()
        return if (len > 1e-6f) Vec2(x / len, y / len) else Vec2(1f, 0f)
    }

    public fun distanceTo(other: Vec2): Float = (this - other).len()

    /** Perpendicular vector rotated 90° counter-clockwise (left) */
    public fun perpLeft(): Vec2 = Vec2(-y, x)

    /** Perpendicular vector rotated 90° clockwise (right) */
    public fun perpRight(): Vec2 = Vec2(y, -x)

    public fun safeNormalized(fallback: Vec2 = Vec2(0f, 1f)): Vec2 {
        val l = len()
        return if (l > EPS) this * (1f / l) else fallback
    }

    public fun half(): Vec2 = this * HALF

    private companion object {

        const val EPS = 1e-5f
        const val HALF = 0.5f
    }
}
