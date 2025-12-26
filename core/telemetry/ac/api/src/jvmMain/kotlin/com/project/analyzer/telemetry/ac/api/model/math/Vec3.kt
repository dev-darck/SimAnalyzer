package com.project.analyzer.telemetry.ac.api.model.math

import kotlin.math.sqrt

public data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    public operator fun minus(o: Vec3): Vec3 = Vec3(x - o.x, y - o.y, z - o.z)
    public fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
    public fun lengthXZ(): Float = sqrt(x * x + z * z)
}
