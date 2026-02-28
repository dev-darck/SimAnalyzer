package com.project.analyzer.telemetry.ac.api.model.calibration

import com.project.analyzer.math.MathEps
import com.project.analyzer.math.Vec2
import kotlin.math.sqrt

/**
 * Lightweight orthonormal 2D frame for a gate:
 * - [forward] points "through" the gate
 * - [normal] points across the gate width (perpendicular to forward)
 */
public data class GateGeometry(val center: Vec2, val forward: Vec2, val normal: Vec2, val halfWidthMeters: Float) {

    /** Gate segment endpoints across width. */
    public fun segment(): Pair<Vec2, Vec2> {
        val dx = normal.x * halfWidthMeters
        val dy = normal.y * halfWidthMeters
        val a = Vec2(center.x + dx, center.y + dy)
        val b = Vec2(center.x - dx, center.y - dy)
        return a to b
    }
}

/**
 * Builds an orthonormal 2D frame from the stored gate vectors.
 *
 * We "fix" the stored normal to be perpendicular to forward, so downstream geometry can assume an orthonormal basis.
 */
public fun Gate.frame2D(fallbackForward: Vec2 = Vec2.Up): GateGeometry {
    val c = centerV2()
    val gf = forwardV2().safeNormalized(fallbackForward)

    val n0 = normalV2()
    val projection = n0.x * gf.x + n0.y * gf.y
    val orthoX = n0.x - gf.x * projection
    val orthoY = n0.y - gf.y * projection
    val nOrtho = safeNormalizedOrFallback(
        x = orthoX,
        y = orthoY,
        fallbackX = -gf.y,
        fallbackY = gf.x,
    )

    return GateGeometry(
        center = c,
        forward = gf,
        normal = nOrtho,
        halfWidthMeters = halfWidthMeters,
    )
}

private fun safeNormalizedOrFallback(
    x: Float,
    y: Float,
    fallbackX: Float,
    fallbackY: Float,
): Vec2 {
    val len2 = x * x + y * y
    if (len2 > MathEps.EPS * MathEps.EPS) {
        val inv = 1f / sqrt(len2)
        return Vec2(x * inv, y * inv)
    }

    val fallbackLen2 = fallbackX * fallbackX + fallbackY * fallbackY
    if (fallbackLen2 > MathEps.EPS * MathEps.EPS) {
        val inv = 1f / sqrt(fallbackLen2)
        return Vec2(fallbackX * inv, fallbackY * inv)
    }
    return Vec2.Up
}
