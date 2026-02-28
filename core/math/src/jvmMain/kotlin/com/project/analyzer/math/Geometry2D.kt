package com.project.analyzer.math

import kotlin.math.abs
import kotlin.math.max

/**
 * Canonical 2D geometry utilities (segment intersection, projections, etc.).
 *
 * All functions are pure and allocation-light.
 */
public object Geometry2D {

    public data class SegmentIntersection(
        /** Param on segment P: p(t) = p0 + t*(p1-p0) */
        val t: Float,
        /** Param on segment Q: q(u) = q0 + u*(q1-q0) */
        val u: Float,
    )

    /**
     * Intersection params for two segments:
     * - P: p0 -> p1
     * - Q: q0 -> q1
     *
     * Returns null if:
     * - segments are (near) parallel, or
     * - intersection point is outside either segment (with tolerance).
     */
    public fun intersectSegmentsParams(
        p0: Vec2,
        p1: Vec2,
        q0: Vec2,
        q1: Vec2,
        epsParallel: Float = MathEps.PARALLEL,
        epsParam: Float = MathEps.PARAM,
    ): SegmentIntersection? {
        val rx = p1.x - p0.x
        val ry = p1.y - p0.y
        val sx = q1.x - q0.x
        val sy = q1.y - q0.y
        val rxs = rx * sy - ry * sx
        if (abs(rxs) < epsParallel) return null

        val qmpx = q0.x - p0.x
        val qmpy = q0.y - p0.y
        val tRaw = (qmpx * sy - qmpy * sx) / rxs
        val uRaw = (qmpx * ry - qmpy * rx) / rxs

        val tIn = (tRaw >= -epsParam && tRaw <= 1f + epsParam)
        val uIn = (uRaw >= -epsParam && uRaw <= 1f + epsParam)
        if (!tIn || !uIn) return null

        return SegmentIntersection(t = tRaw, u = uRaw)
    }

    public fun pointOnSegment(p0: Vec2, p1: Vec2, t: Float): Vec2 = Vec2(
        x = p0.x + (p1.x - p0.x) * t,
        y = p0.y + (p1.y - p0.y) * t,
    )

    /**
     * True when moving from p0->p1 crosses the gate plane (through [center] with axis [forward])
     * in the "forward" direction.
     *
     * This is intentionally side-based (d0 < 0 and d1 >= 0) and does NOT require an exact hit.
     */
    public fun isForwardCrossing(
        p0: Vec2,
        p1: Vec2,
        center: Vec2,
        forward: Vec2,
        dirEps: Float = MathEps.DIR,
    ): Boolean {
        val d0 = forward.x * (p0.x - center.x) + forward.y * (p0.y - center.y)
        val d1 = forward.x * (p1.x - center.x) + forward.y * (p1.y - center.y)
        return (d0 < -dirEps) && (d1 >= -dirEps)
    }

    /** How far point [p] lies outside the band |dot(p-center, normal)| <= halfWidth. */
    public fun outsideBandByNormal(p: Vec2, center: Vec2, normal: Vec2, halfWidth: Float): Float {
        val along = (p.x - center.x) * normal.x + (p.y - center.y) * normal.y
        return max(0f, abs(along) - halfWidth)
    }
}
