package com.project.analyzer.math

import kotlin.math.cos
import kotlin.math.sin

/**
 * Helpers for "heading" / yaw angle math in XZ plane.
 *
 * NOTE: current project convention uses (sin(rad), cos(rad)) for forward in XZ plane.
 */
public object Heading2D {

    /** Forward direction in XZ plane using the project's convention. */
    public fun forwardFromRad(headingRad: Float, fallback: Vec2 = Vec2.Up): Vec2 =
        Vec2(sin(headingRad), cos(headingRad)).safeNormalized(fallback)

    /**
     * Resolves ambiguity for a heading direction: choose between [a] and its opposite, based on [prefer].
     * If [prefer] is null, returns [a].
     */
    public fun resolveBidirectional(a: Vec2, prefer: Vec2?, fallback: Vec2 = Vec2.Up): Vec2 {
        val ref = prefer?.safeNormalized(fallback) ?: return a
        val b = a * -1f
        return if (a.dot(ref) >= b.dot(ref)) a else b
    }

    /**
     * Local (vx, vz) -> world XZ using the same rotation as existing physics extractor.
     * Returns (worldVx, worldVz).
     */
    public fun localToWorldXZ(localVx: Float, localVz: Float, headingRad: Float): Vec2 {
        val ch = cos(headingRad)
        val sh = sin(headingRad)
        val worldVx = localVx * ch + localVz * sh
        val worldVz = -localVx * sh + localVz * ch
        return Vec2(worldVx, worldVz)
    }

    /**
     * PoseExtractor historically uses TWO candidates:
     *  A = (sin(rad), cos(rad))
     *  B = (-sin(rad), cos(rad))   <-- intentionally preserved even though it's not the exact opposite.
     *
     * Keep this function to avoid changing existing behavior.
     */
    public fun resolveFromRadiansPoseExtractor(headingRad: Float, prefer: Vec2?, fallback: Vec2 = Vec2.Up): Vec2 {
        val a = Vec2(sin(headingRad), cos(headingRad)).safeNormalized(fallback)
        val b = Vec2(-sin(headingRad), cos(headingRad)).safeNormalized(fallback)
        val ref = prefer?.safeNormalized(fallback) ?: return a
        return if (a.dot(ref) >= b.dot(ref)) a else b
    }
}
