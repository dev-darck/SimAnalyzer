package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.math.Vec2
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.abs
import kotlin.math.max

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

        val c = gate.centerV2()

        val gf = gate.forwardV2().safeNormalized(Vec2(0f, 1f))

        var gn = gate.normalV2()
        gn = (gn - gf * gn.dot(gf)).safeNormalized(gf.perpLeft())

        val half = gate.halfWidthMeters

        val a = c + gn * half
        val b = c - gn * half

        val p0 = previousPose.position
        val p1 = currentPose.position

        val r = p1 - p0
        val dpLen = r.len()
        if (dpLen < MIN_MOVEMENT_METERS) return null

        val d0 = gf.dot(p0 - c)
        val d1 = gf.dot(p1 - c)

        val s = b - a
        val rxs = cross(r, s)

        if (abs(rxs) < EPS) return null

        val qmp = a - p0
        val tRaw = cross(qmp, s) / rxs
        val uRaw = cross(qmp, r) / rxs

        val tIn = (tRaw >= -EPS_T && tRaw <= 1f + EPS_T)
        val uIn = (uRaw >= -EPS_T && uRaw <= 1f + EPS_T)

        if (!tIn || !uIn) return null

        val t = tRaw.coerceIn(0f, 1f)
        val hit = p0 + r * t

        val isForward = (d0 < -DIR_EPS) && (d1 >= -DIR_EPS)

        val along = (hit - c).dot(gn)
        val outsideBy = max(0f, abs(along) - half)

        return GateCrossing(
            interpolationFactor = t,
            isForwardDirection = isForward,
            hitPoint = hit,
            outsideByMeters = outsideBy
        )
    }

    private fun cross(a: Vec2, b: Vec2): Float = a.x * b.y - a.y * b.x

    private companion object {

        const val EPS = 1e-6f
        const val EPS_T = 1e-4f
        const val DIR_EPS = 1e-4f
        const val MIN_MOVEMENT_METERS = 0.002f
    }
}
