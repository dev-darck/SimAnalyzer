package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.calibration.data.model.Gate
import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.calibration.data.model.Vec2
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay
import kotlin.math.acos
import kotlin.math.sqrt

class GateCaptureException(message: String) : IllegalStateException(message)

@Inject
@SingleIn(AppScope::class)
class CaptureGateOnStandstillUseCase(
    private val provider: TelemetrySampleProvider,
    private val buildGate: BuildGateUseCase,
) {

    suspend fun capture(
        triggerRadiusMeters: Float = 25f,
        debugHalfWidthMeters: Float = 30f,

        waitStableMs: Long = 400L,
        captureMs: Long = 900L,

        speedThresholdKmh: Float = 0.5f,

        maxPosStdMeters: Float = 0.15f,
        maxHeadingStdDeg: Float = 1.2f,
    ): Gate {

        val first = provider.sample.value
        val anchorPose = first.pose ?: throw GateCaptureException("No pose. Wait telemetry.")
        if (first.speedKmh > speedThresholdKmh) {
            throw GateCaptureException("Stop on the line, then press capture (speed=%.1f km/h)".format(first.speedKmh))
        }

        var stable = 0L
        while (stable < waitStableMs) {
            val s = provider.sample.value
            val pose = s.pose ?: throw GateCaptureException("No pose. Wait telemetry.")
            if (s.speedKmh > speedThresholdKmh) {
                throw GateCaptureException("Car moved. Stop and press again (speed=%.1f)".format(s.speedKmh))
            }
            val drift = (pose.pos - anchorPose.pos).length()
            if (drift > maxPosStdMeters * 3) {
                throw GateCaptureException("Car drifted %.2fm from capture point. Stop stable.".format(drift))
            }
            stable += 50L
            delay(50L)
        }

        val window = ArrayList<Pose2D>(32)
        window.add(anchorPose)
        var t = 0L
        while (t < captureMs) {
            val s = provider.sample.value
            val pose = s.pose ?: throw GateCaptureException("No pose")
            if (s.speedKmh > speedThresholdKmh) {
                throw GateCaptureException("Car moved during capture. Stop and press again.")
            }
            window.add(pose)
            delay(50L)
            t += 50L
        }

        val avgPose = average(window)
        val posStd = positionStd(window, avgPose.pos)
        val headingStd = headingStdDeg(window, avgPose.forward)

        if (posStd > maxPosStdMeters) {
            throw GateCaptureException("Position noise (std=%.2fm). Stop stable.".format(posStd))
        }
        if (headingStd > maxHeadingStdDeg) {
            throw GateCaptureException("Direction noise (std=%.1f°). Stop stable.".format(headingStd))
        }

        return buildGate.fromPose(
            pose = avgPose,
            triggerRadiusMeters = triggerRadiusMeters,
            debugHalfWidthMeters = debugHalfWidthMeters,
        )
    }

    private fun average(samples: List<Pose2D>): Pose2D {
        var sx = 0f
        var sy = 0f
        var fx = 0f
        var fy = 0f
        for (p in samples) {
            sx += p.pos.x
            sy += p.pos.y
            fx += p.forward.x
            fy += p.forward.y
        }
        val n = samples.size.toFloat()
        val pos = Vec2(sx / n, sy / n)
        val f = Vec2(fx / n, fy / n).normalized()
        return Pose2D(pos, f)
    }

    private fun positionStd(samples: List<Pose2D>, mean: Vec2): Float {
        var acc = 0f
        for (p in samples) {
            val dx = p.pos.x - mean.x
            val dy = p.pos.y - mean.y
            acc += dx * dx + dy * dy
        }
        return sqrt(acc / samples.size)
    }

    private fun headingStdDeg(samples: List<Pose2D>, meanForward: Vec2): Float {
        val mean = meanForward.normalized()
        var acc = 0f
        for (p in samples) {
            val f = p.forward.normalized()
            val dot = (f.dot(mean)).coerceIn(-1f, 1f)
            val angleRad = acos(dot)
            val deg = angleRad * 57.29578f
            acc += deg * deg
        }
        return sqrt(acc / samples.size)
    }
}
