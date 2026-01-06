package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.Vec2Dto
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class BuildGateUseCase {

    /**
     * Builds a [Gate] from a 2D pose.
     *
     * - [Pose2D.pos] becomes the gate center.
     * - [Pose2D.forward] becomes the gate forward direction (normalized, with fallback).
     * - Gate normal is derived as a strict left-perpendicular to forward.
     */
    fun fromPose(
        pose: Pose2D,
        halfWidthMeters: Float = 10f,
    ): Gate {
        val forward = pose.forward.safeNormalized(Vec2.Up)
        val normal = forward.perpLeft()

        return Gate.create(
            center = pose.pos,
            forward = forward,
            normal = normal,
            halfWidthMeters = halfWidthMeters
        )
    }
}

/** Flips the gate direction (180°): forward and normal are negated. */
fun Gate.flipDirection(): Gate {
    val f = forwardV2() * -1f
    val n = normalV2() * -1f

    return copy(
        forward = Vec2Dto.from(f),
        normal = Vec2Dto.from(n)
    )
}
