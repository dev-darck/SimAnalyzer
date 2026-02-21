package com.project.analyzer.telemetry.ac.api.model.calibration

import com.project.analyzer.math.Vec2

/**
 * Lightweight orthonormal 2D frame for a gate:
 * - [forward] points "through" the gate
 * - [normal] points across the gate width (perpendicular to forward)
 */
public data class GateGeometry(val center: Vec2, val forward: Vec2, val normal: Vec2, val halfWidthMeters: Float) {

    /** Gate segment endpoints across width. */
    public fun segment(): Pair<Vec2, Vec2> {
        val a = center + normal * halfWidthMeters
        val b = center - normal * halfWidthMeters
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
    val nOrtho = (n0 - gf * n0.dot(gf)).safeNormalized(gf.perpLeft())

    return GateGeometry(
        center = c,
        forward = gf,
        normal = nOrtho,
        halfWidthMeters = halfWidthMeters,
    )
}
