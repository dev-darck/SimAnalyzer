package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.Vec2Dto

internal interface BuildGateUseCase {

    fun fromPose(pose: Pose2D, halfWidthMeters: Float = 10f): Gate
}

/** Flips the gate direction (180°): forward and normal are negated. */
fun Gate.flipDirection(): Gate {
    val f = forwardV2() * -1f
    val n = normalV2() * -1f

    return copy(
        forward = Vec2Dto.from(f),
        normal = Vec2Dto.from(n),
    )
}
