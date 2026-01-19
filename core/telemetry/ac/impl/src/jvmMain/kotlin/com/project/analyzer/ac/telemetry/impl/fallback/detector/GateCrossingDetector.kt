package com.project.analyzer.ac.telemetry.impl.fallback.detector

import com.project.analyzer.ac.telemetry.impl.fallback.detector.model.GateCrossing
import com.project.analyzer.ac.telemetry.impl.fallback.pose.model.CarPose
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.math.Geometry2D
import com.project.analyzer.math.MathEps
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.frame2D
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class GateCrossingDetector {

    fun detectCrossing(
        previousPose: CarPose,
        currentPose: CarPose,
        gate: Gate,
    ): GateCrossing? {

        val p0 = previousPose.position
        val p1 = currentPose.position
        val dp = p1 - p0

        if (dp.len2() < MIN_MOVEMENT_METERS * MIN_MOVEMENT_METERS) return null

        val frame = gate.frame2D(fallbackForward = Vec2.Companion.Up)
        val (a, b) = frame.segment()

        val hitParams = Geometry2D.intersectSegmentsParams(
            p0 = p0,
            p1 = p1,
            q0 = a,
            q1 = b,
            epsParallel = MathEps.PARALLEL,
            epsParam = MathEps.PARAM,
        ) ?: return null

        val t = hitParams.t.coerceIn(0f, 1f)
        val hit = p0 + dp * t

        val isForward = Geometry2D.isForwardCrossing(
            p0 = p0,
            p1 = p1,
            center = frame.center,
            forward = frame.forward,
            dirEps = MathEps.DIR,
        )

        val outsideBy = Geometry2D.outsideBandByNormal(
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
