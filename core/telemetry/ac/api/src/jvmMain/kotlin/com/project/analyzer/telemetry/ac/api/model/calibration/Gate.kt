package com.project.analyzer.telemetry.ac.api.model.calibration

import com.project.analyzer.math.Vec2
import kotlinx.serialization.Serializable

@Serializable
public data class Vec2Dto(val x: Float, val y: Float) {
    public fun toVec2(): Vec2 = Vec2(x, y)
    public companion object {
        public fun from(v: Vec2): Vec2Dto = Vec2Dto(v.x, v.y)
    }
}

/**
 * A timing gate - a line segment across the track.
 *
 * @param center Center point of the line (where car stopped during calibration)
 * @param forward Direction perpendicular to the line (direction car crosses)
 * @param normal Direction along the line (left-right)
 * @param halfWidthMeters Half-width of the line segment (distance from center to each end)
 */
@Serializable
public data class Gate(
    val center: Vec2Dto,
    val forward: Vec2Dto,
    val normal: Vec2Dto,
    val halfWidthMeters: Float = 8f
) {
    public fun centerV2(): Vec2 = center.toVec2()
    public fun forwardV2(): Vec2 = forward.toVec2()
    public fun normalV2(): Vec2 = normal.toVec2()

    public companion object {
        public fun create(
            center: Vec2,
            forward: Vec2,
            normal: Vec2,
            halfWidthMeters: Float = 8f
        ): Gate = Gate(
            center = Vec2Dto.from(center),
            forward = Vec2Dto.from(forward),
            normal = Vec2Dto.from(normal),
            halfWidthMeters = halfWidthMeters
        )
    }
}
