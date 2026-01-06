package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.math.Geometry2D.intersectSegmentsParams
import com.project.analyzer.math.Geometry2D.isForwardCrossing
import com.project.analyzer.math.Geometry2D.outsideBandByNormal
import com.project.analyzer.math.MathEps
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.frame2D
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class GateCrossingDetector {

    data class GateCrossing(
        val interpolationFactor: Float,
        val isForwardDirection: Boolean,
        val hitPoint: Vec2,
        val outsideByMeters: Float,
    )

    fun detectCrossing(
        previousPose: CarPose,
        currentPose: CarPose,
        gate: Gate,
    ): GateCrossing? {

        val p0 = previousPose.position
        val p1 = currentPose.position
        val dp = p1 - p0

        if (dp.len2() < MIN_MOVEMENT_METERS * MIN_MOVEMENT_METERS) return null

        val frame = gate.frame2D(fallbackForward = Vec2.Up)
        val (a, b) = frame.segment()

        val hitParams = intersectSegmentsParams(
            p0 = p0,
            p1 = p1,
            q0 = a,
            q1 = b,
            epsParallel = MathEps.PARALLEL,
            epsParam = MathEps.PARAM,
        ) ?: return null

        val t = hitParams.t.coerceIn(0f, 1f)
        val hit = p0 + dp * t

        val isForward = isForwardCrossing(
            p0 = p0,
            p1 = p1,
            center = frame.center,
            forward = frame.forward,
            dirEps = MathEps.DIR,
        )

        val outsideBy = outsideBandByNormal(
            p = hit,
            center = frame.center,
            normal = frame.normal,
            halfWidth = frame.halfWidthMeters
        )

        return GateCrossing(
            interpolationFactor = t,
            isForwardDirection = isForward,
            hitPoint = hit,
            outsideByMeters = outsideBy
        )
    }

    private companion object {
        const val MIN_MOVEMENT_METERS = 0.002f
    }
}
