package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.math.Vec2
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GateCaptureException(message: String) : IllegalStateException(message)

data class GateCaptureResult(
    val gate: Gate,
    val capturedPosition: Vec2,
    val capturedForward: Vec2,
    val sampleCount: Int,
    val positionStdMeters: Float,
)

@Inject
@SingleIn(ScreenScope::class)
class CaptureGateOnStandstillUseCase(
    private val provider: TelemetrySampleProvider,
    private val buildGate: BuildGateUseCase,
) {

    suspend fun captureWithDetails(
        halfWidthMeters: Float = 10f,
        waitStableMs: Long = 600L,
        captureMs: Long = 1500L,
        speedThresholdKmh: Float = 2f,
        maxDriftMeters: Float = 0.5f,
        maxPosStdMeters: Float = 0.05f,
    ): GateCaptureResult {

        val first = provider.sample.value
        val anchor = first.pose ?: throw GateCaptureException("No telemetry data. Make sure game is running.")

        val stableIntervalMs = 25L
        var stableTime = 0L

        var firstPos: Vec2? = null
        var lastPos: Vec2? = null

        while (stableTime < waitStableMs) {
            val s = provider.sample.value
            val pose = s.pose ?: throw GateCaptureException("Lost telemetry data.")

            val drift = (pose.pos - anchor.pos).len()
            if (drift > maxDriftMeters) {
                throw GateCaptureException("Car drifted %.2fm from capture point.".format(drift))
            }

            if (firstPos == null) firstPos = pose.pos
            lastPos = pose.pos

            if (s.speedKmh <= speedThresholdKmh) {
                stableTime += stableIntervalMs
            } else {
                stableTime = 0L
            }

            delay(stableIntervalMs)
        }

        val netDelta = (lastPos!! - firstPos!!)
        val netLen = netDelta.len()

        val moveDirRaw: Vec2? =
            if (netLen >= MIN_NET_DISPLACEMENT_METERS) netDelta * (1f / netLen) else null

        val captureIntervalMs = 10L
        var captureTime = 0L
        val samples = ArrayList<Vec2>(256)

        while (captureTime < captureMs) {
            val s = provider.sample.value
            val pose = s.pose ?: throw GateCaptureException("Lost telemetry data during capture.")
            if (s.speedKmh > speedThresholdKmh) throw GateCaptureException("Car moved during capture.")

            samples.add(pose.pos)

            captureTime += captureIntervalMs
            delay(captureIntervalMs)
        }

        if (samples.size < 20) throw GateCaptureException("Not enough samples: ${samples.size}")

        val avgPos = mean2(samples)
        val posStd = std2(samples, avgPos)
        if (posStd > maxPosStdMeters) {
            throw GateCaptureException(
                "Position noise too high (std=%.3fm, max=%.3fm).".format(posStd, maxPosStdMeters)
            )
        }

        val cur = provider.sample.value

        val axleForward = cur.axleForward?.safeNormalized(Vec2(0f, 1f))

        val headingForward = Vec2(sin(cur.headingRad), cos(cur.headingRad)).safeNormalized(Vec2(0f, 1f))

        val moveDir = validateAndFixMoveDir(
            moveDir = moveDirRaw,
            axleForward = axleForward,
            headingForward = headingForward
        )

        val forward = (moveDir ?: axleForward ?: headingForward).safeNormalized(Vec2(0f, 1f))

        val gate = buildGate.fromPose(Pose2D(avgPos, forward), halfWidthMeters)

        return GateCaptureResult(
            gate = gate,
            capturedPosition = avgPos,
            capturedForward = forward,
            sampleCount = samples.size,
            positionStdMeters = posStd,
        )
    }

    private fun validateAndFixMoveDir(
        moveDir: Vec2?,
        axleForward: Vec2?,
        headingForward: Vec2
    ): Vec2? {
        if (moveDir == null) return null

        val ref = axleForward ?: headingForward
        val d = moveDir.dot(ref)

        if (abs(d) < 0.3f) return null

        return if (d < -0.7f) moveDir * -1f else moveDir
    }

    private fun mean2(samples: List<Vec2>): Vec2 {
        var sx = 0.0
        var sy = 0.0
        for (p in samples) {
            sx += p.x
            sy += p.y
        }
        val n = samples.size.toDouble()
        return Vec2((sx / n).toFloat(), (sy / n).toFloat())
    }

    private fun std2(samples: List<Vec2>, mean: Vec2): Float {
        var acc = 0.0
        for (p in samples) {
            val dx = (p.x - mean.x).toDouble()
            val dy = (p.y - mean.y).toDouble()
            acc += dx * dx + dy * dy
        }
        return sqrt(acc / samples.size).toFloat()
    }

    private companion object {

        const val MIN_NET_DISPLACEMENT_METERS = 0.15f
    }
}
