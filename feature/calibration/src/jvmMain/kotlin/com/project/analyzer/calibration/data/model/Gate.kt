package com.project.analyzer.calibration.data.model

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
data class Gate(
    val center: Vec2,
    val forward: Vec2,
    val normal: Vec2,
    // triggerRadius: half-length of stripe along forward; debugHalfWidth: half-width across track
    val triggerRadiusMeters: Float = 2.5f,
    val debugHalfWidthMeters: Float = 10f
)

@Serializable
data class Vec2(
    val x: Float,
    val y: Float,
) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(k: Float) = Vec2(x * k, y * k)

    fun dot(o: Vec2): Float = x * o.x + y * o.y
    fun length(): Float = sqrt(x * x + y * y)

    fun normalized(): Vec2 {
        val len = length()
        return if (len > 1e-6f) Vec2(x / len, y / len) else Vec2(1f, 0f)
    }

    fun perpLeft(): Vec2 = Vec2(-y, x)
}
