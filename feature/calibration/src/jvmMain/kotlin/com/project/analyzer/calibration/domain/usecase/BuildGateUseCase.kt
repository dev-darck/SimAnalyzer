package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.Vec2Dto
import com.project.analyzer.telemetry.ac.api.model.math.Vec2
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class BuildGateUseCase {

    fun fromPose(
        pose: Pose2D,
        halfWidthMeters: Float = 10f,
    ): Gate {
        val forward = pose.forward.safeNormalized(Vec2(0f, 1f))
        val normal = forward.perpLeft()

        return Gate.create(
            center = pose.pos,
            forward = forward,
            normal = normal,
            halfWidthMeters = halfWidthMeters
        )
    }
}

fun Gate.flipDirection(): Gate {
    val f = forwardV2() * -1f
    val n = normalV2() * -1f

    return copy(
        forward = Vec2Dto.from(f),
        normal = Vec2Dto.from(n)
    )
}
