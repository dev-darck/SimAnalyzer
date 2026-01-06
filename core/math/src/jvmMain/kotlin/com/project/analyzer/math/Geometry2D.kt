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
        val r = p1 - p0
        val s = q1 - q0

        val rxs = r cross s
        if (abs(rxs) < epsParallel) return null

        val qmp = q0 - p0
        val tRaw = (qmp cross s) / rxs
        val uRaw = (qmp cross r) / rxs

        val tIn = (tRaw >= -epsParam && tRaw <= 1f + epsParam)
        val uIn = (uRaw >= -epsParam && uRaw <= 1f + epsParam)
        if (!tIn || !uIn) return null

        return SegmentIntersection(t = tRaw, u = uRaw)
    }

    public fun pointOnSegment(p0: Vec2, p1: Vec2, t: Float): Vec2 = p0 + (p1 - p0) * t

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
        val d0 = forward.dot(p0 - center)
        val d1 = forward.dot(p1 - center)
        return (d0 < -dirEps) && (d1 >= -dirEps)
    }

    /** How far point [p] lies outside the band |dot(p-center, normal)| <= halfWidth. */
    public fun outsideBandByNormal(
        p: Vec2,
        center: Vec2,
        normal: Vec2,
        halfWidth: Float
    ): Float {
        val along = (p - center).dot(normal)
        return max(0f, abs(along) - halfWidth)
    }
}
