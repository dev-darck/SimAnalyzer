package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.math.Heading2D
import com.project.analyzer.math.Statistics2D
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

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
        val moveDirRaw = awaitStableStandstill(
            waitStableMs = waitStableMs,
            speedThresholdKmh = speedThresholdKmh,
            maxDriftMeters = maxDriftMeters,
        )

        val samples = collectPositionSamples(
            captureMs = captureMs,
            speedThresholdKmh = speedThresholdKmh,
        )

        if (samples.size < MIN_SAMPLES) throw GateCaptureException("Not enough samples: ${samples.size}")

        // Average + position noise check.
        val avgPos = Statistics2D.mean(samples)
        val posStd = Statistics2D.std(samples, avgPos)
        if (posStd > maxPosStdMeters) {
            throw GateCaptureException(
                "Position noise too high (std=%.3fm, max=%.3fm).".format(posStd, maxPosStdMeters),
            )
        }

        // Determine the forward direction for the gate.
        // Priority: validated moveDir (from net displacement) -> axleForward (if available) -> headingForward.
        val cur = provider.sample.value
        val axleForward = cur.axleForward?.safeNormalized(Vec2.Up)
        val headingForward = Heading2D.forwardFromRad(cur.headingRad)

        val moveDir = validateAndFixMoveDir(
            moveDir = moveDirRaw,
            axleForward = axleForward,
            headingForward = headingForward,
        )

        val forward = (moveDir ?: axleForward ?: headingForward).safeNormalized(Vec2.Up)

        // Build the gate.
        val gate = buildGate.fromPose(Pose2D(avgPos, forward), halfWidthMeters)

        return GateCaptureResult(
            gate = gate,
            capturedPosition = avgPos,
            capturedForward = forward,
            sampleCount = samples.size,
            positionStdMeters = posStd,
        )
    }

    /**
     * Waits until the car stays under [speedThresholdKmh] continuously for [waitStableMs],
     * while also ensuring it doesn't drift from the first observed pose by more than [maxDriftMeters].
     */
    private suspend fun awaitStableStandstill(
        waitStableMs: Long,
        speedThresholdKmh: Float,
        maxDriftMeters: Float,
    ): Vec2? {
        val first = provider.sample.value
        val anchor = first.pose ?: throw GateCaptureException("No telemetry data. Make sure game is running.")

        var stableTimeMs = 0L
        var firstPos: Vec2? = null
        var lastPos: Vec2? = null

        while (stableTimeMs < waitStableMs) {
            val s = provider.sample.value
            val pose = s.pose ?: throw GateCaptureException("Lost telemetry data.")

            val drift = (pose.pos - anchor.pos).len()
            if (drift > maxDriftMeters) {
                throw GateCaptureException("Car drifted %.2fm from capture point.".format(drift))
            }

            if (firstPos == null) firstPos = pose.pos
            lastPos = pose.pos

            stableTimeMs = if (s.speedKmh <= speedThresholdKmh) {
                stableTimeMs + STABLE_INTERVAL_MS
            } else {
                0L
            }

            delay(STABLE_INTERVAL_MS)
        }

        val netDelta = (lastPos!! - firstPos!!)
        val netLen = netDelta.len()
        val moveDirRaw = if (netLen >= MIN_NET_DISPLACEMENT_METERS) netDelta * (1f / netLen) else null

        return moveDirRaw
    }

    /** Collects position samples for [captureMs] while ensuring the car remains below a speed threshold. */
    private suspend fun collectPositionSamples(captureMs: Long, speedThresholdKmh: Float): List<Vec2> {
        var captureTimeMs = 0L
        val expected = ((captureMs + CAPTURE_INTERVAL_MS - 1) / CAPTURE_INTERVAL_MS).toInt()
        val samples = ArrayList<Vec2>(expected)

        while (captureTimeMs < captureMs) {
            val s = provider.sample.value
            val pose = s.pose ?: throw GateCaptureException("Lost telemetry data during capture.")
            if (s.speedKmh > speedThresholdKmh) throw GateCaptureException("Car moved during capture.")

            samples.add(pose.pos)

            captureTimeMs += CAPTURE_INTERVAL_MS
            delay(CAPTURE_INTERVAL_MS)
        }

        return samples
    }

    private fun validateAndFixMoveDir(moveDir: Vec2?, axleForward: Vec2?, headingForward: Vec2): Vec2? {
        if (moveDir == null) return null

        val ref = axleForward ?: headingForward
        val d = moveDir.dot(ref)

        if (abs(d) < 0.3f) return null

        return if (d < -0.7f) moveDir * -1f else moveDir
    }

    private companion object {

        const val MIN_NET_DISPLACEMENT_METERS = 0.15f

        val STABLE_INTERVAL_MS = 25.milliseconds.inWholeMilliseconds
        val CAPTURE_INTERVAL_MS = 10.milliseconds.inWholeMilliseconds
        const val MIN_SAMPLES = 20
    }
}
