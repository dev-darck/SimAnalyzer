package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.calibration.data.model.Gate
import com.project.analyzer.calibration.data.model.Pose2D
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class BuildGateUseCase {

    fun fromPose(
        pose: Pose2D,
        triggerRadiusMeters: Float,
        debugHalfWidthMeters: Float,
    ): Gate {
        val forward = pose.forward.normalized()
        val normal = forward.perpLeft().normalized()
        return Gate(
            center = pose.pos,
            forward = forward,
            normal = normal,
            triggerRadiusMeters = triggerRadiusMeters,
            debugHalfWidthMeters = debugHalfWidthMeters
        )
    }
}
