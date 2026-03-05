package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
internal class BuildGateUseCaseImpl : BuildGateUseCase {

    /**
     * Builds a [Gate] from a 2D pose.
     *
     * - [Pose2D.pos] becomes the gate center.
     * - [Pose2D.forward] becomes the gate forward direction (normalized, with fallback).
     * - Gate normal is derived as a strict left-perpendicular to forward.
     */
    override fun fromPose(pose: Pose2D, halfWidthMeters: Float): Gate {
        val forward = pose.forward.safeNormalized(Vec2.Up)
        val normal = forward.perpLeft()

        return Gate.create(
            center = pose.pos,
            forward = forward,
            normal = normal,
            halfWidthMeters = halfWidthMeters,
        )
    }
}
