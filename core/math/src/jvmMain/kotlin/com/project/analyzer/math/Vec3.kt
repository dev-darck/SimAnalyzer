package com.project.analyzer.math

import kotlin.math.abs
import kotlin.math.sqrt

public data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float
) {

    public operator fun minus(o: Vec3): Vec3 = Vec3(x - o.x, y - o.y, z - o.z)
    public operator fun plus(o: Vec3): Vec3 = Vec3(x + o.x, y + o.y, z + o.z)
    public operator fun times(s: Float): Vec3 = Vec3(x * s, y * s, z * s)

    public fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z

    /** Length of the vector projection on XZ plane. */
    public fun lengthXZ(): Float = sqrt(x * x + z * z)
}

/**
 * Converts a Vec3 to Vec2 using XZ plane. Returns null if coordinates are invalid.
 */
public fun Vec3.toVec2XZIfValid(maxAbsCoordinate: Float): Vec2? {
    if (!x.isFinite() || !z.isFinite()) return null
    if (x == 0f && z == 0f) return null
    if (abs(x) > maxAbsCoordinate || abs(z) > maxAbsCoordinate) return null
    return Vec2(x, z)
}
