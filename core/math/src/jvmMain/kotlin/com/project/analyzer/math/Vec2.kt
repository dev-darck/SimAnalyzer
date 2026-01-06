package com.project.analyzer.math

import com.project.analyzer.math.MathEps.EPS
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lightweight 2D vector for XZ-plane world math (x, z) or generic 2D use.
 *
 * Conventions:
 * - `perpLeft()` rotates +90° (counter-clockwise): (x, y) -> (-y, x)
 * - `perpRight()` rotates -90° (clockwise):      (x, y) -> (y, -x)
 */
public data class Vec2(
    val x: Float,
    val y: Float
) {

    public operator fun plus(o: Vec2): Vec2 = Vec2(x + o.x, y + o.y)
    public operator fun minus(o: Vec2): Vec2 = Vec2(x - o.x, y - o.y)
    public operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)
    public operator fun div(scalar: Float): Vec2 = Vec2(x / scalar, y / scalar)

    public fun dot(o: Vec2): Float = x * o.x + y * o.y

    /** 2D cross product magnitude (scalar): ax*by - ay*bx. */
    public infix fun cross(o: Vec2): Float = x * o.y - y * o.x

    public fun len2(): Float = x * x + y * y
    public fun len(): Float = sqrt(len2())

    public fun distanceTo(other: Vec2): Float = (this - other).len()

    /** Returns the vector normalized, or (1,0) if its length is too small. */
    public fun normalized(): Vec2 {
        val l = len()
        return if (l > MathEps.PARALLEL) this * (1f / l) else Vec2(1f, 0f)
    }

    /** Returns the vector normalized, or [fallback] if its length is too small. */
    public fun safeNormalized(fallback: Vec2 = Up): Vec2 {
        val l2 = len2()
        return if (l2 > EPS * EPS) this * (1f / sqrt(l2)) else fallback
    }

    /** Midpoint between this and [other]. */
    public fun midpoint(other: Vec2): Vec2 = (this + other) * HALF

    /** Perpendicular vector rotated 90° counter-clockwise (left). */
    public fun perpLeft(): Vec2 = Vec2(-y, x)

    /** Perpendicular vector rotated 90° clockwise (right). */
    public fun perpRight(): Vec2 = Vec2(y, -x)

    /** Removes the component along a (preferably unit) vector [axis]. */
    public fun rejectFrom(axis: Vec2): Vec2 = this - axis * this.dot(axis)

    /** Component along a (preferably unit) vector [axis]. */
    public fun projectOnto(axis: Vec2): Float = this.dot(axis)

    public fun avg(b: Vec2?): Vec2? = if (b != null) midpoint(b) else this

    public companion object {

        public val Zero: Vec2 = Vec2(0f, 0f)
        public val Right: Vec2 = Vec2(1f, 0f)
        public val Up: Vec2 = Vec2(0f, 1f)
        private const val HALF = 0.5f
    }
}

/** Utility for comparing floats with a tolerance. */
public fun Float.almostZero(eps: Float = MathEps.PARALLEL): Boolean = abs(this) <= eps
