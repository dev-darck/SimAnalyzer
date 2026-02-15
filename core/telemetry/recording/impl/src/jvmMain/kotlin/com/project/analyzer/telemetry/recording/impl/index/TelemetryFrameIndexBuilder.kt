package com.project.analyzer.telemetry.recording.impl.index

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.math.Vec2
import com.project.analyzer.math.toVec2XZIfValid
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.session.PitState
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndex
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndexFlags
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.atan2

@Inject
@SingleIn(SessionScope::class)
class TelemetryFrameIndexBuilder {

    fun build(frame: TelemetryFrame): TelemetryFrameIndex {
        val position = extractPosition(frame)
        val heading = resolveHeading(frame)
        val speed = resolveSpeedKmh(frame)
        val trackPosition = frame.session?.track?.normalizedLapPosition
        val lap = resolveLap(frame)
        val sector = frame.lap?.currentSectorIndex
        val flags = buildFlags(frame)

        return TelemetryFrameIndex(
            positionX = position?.x,
            positionZ = position?.y,
            headingRad = heading,
            speedKmh = speed,
            trackPosition = trackPosition,
            lap = lap,
            sector = sector,
            flags = flags,
        )
    }

    private fun extractPosition(frame: TelemetryFrame): Vec2? {
        frame.car?.worldPosition
            ?.toVec2XZIfValid(MAX_VALID_COORDINATE)
            ?.let { return it }

        val wheels = frame.wheels ?: return null
        val fl = wheels.fl?.contactPoint?.toVec2XZIfValid(MAX_VALID_COORDINATE)
        val fr = wheels.fr?.contactPoint?.toVec2XZIfValid(MAX_VALID_COORDINATE)
        val rl = wheels.rl?.contactPoint?.toVec2XZIfValid(MAX_VALID_COORDINATE)
        val rr = wheels.rr?.contactPoint?.toVec2XZIfValid(MAX_VALID_COORDINATE)

        if (fl == null && fr == null && rl == null && rr == null) return null

        val front = axleCenter(fl, fr)
        val rear = axleCenter(rl, rr)

        return when {
            front != null && rear != null -> front.midpoint(rear)
            front != null -> front
            rear != null -> rear
            else -> null
        }
    }

    private fun axleCenter(a: Vec2?, b: Vec2?): Vec2? = when {
        a != null && b != null -> a.midpoint(b)
        a != null -> a
        b != null -> b
        else -> null
    }

    private fun resolveSpeedKmh(frame: TelemetryFrame): Float? {
        val speed = frame.car?.speedKmh
        if (speed != null) return speed

        val velocity = frame.car?.velocity ?: return null
        val metersPerSecond = velocity.lengthXZ()
        return if (metersPerSecond > 0f) metersPerSecond * MPS_TO_KMH else null
    }

    private fun resolveHeading(frame: TelemetryFrame): Float? {
        val heading = frame.car?.heading
        if (heading != null) return heading

        val v = frame.car?.velocity ?: return null
        if (v.lengthXZ() < MIN_VELOCITY_FOR_HEADING) return null
        return atan2(v.z, v.x)
    }

    private fun resolveLap(frame: TelemetryFrame): Int? {
        val lap = frame.lap?.currentLapIndex
        if (lap != null && lap > 0) return lap

        val completed = frame.session?.completedLaps ?: return null
        return if (completed >= 0) completed + 1 else null
    }

    private fun buildFlags(frame: TelemetryFrame): Int {
        var flags = 0

        val pit = frame.session?.pit
        flags = applyPitFlags(flags, pit)

        if (frame.lap?.validity == LapValidity.INVALID) {
            flags = flags or TelemetryFrameIndexFlags.INVALID_LAP
        }

        return flags
    }

    private fun applyPitFlags(base: Int, pit: PitState?): Int {
        var flags = base

        if (pit?.isInPit == true) {
            flags = flags or TelemetryFrameIndexFlags.IN_PIT
        }
        if (pit?.isInPitLane == true) {
            flags = flags or TelemetryFrameIndexFlags.IN_PIT_LANE
        }

        return flags
    }

    private companion object {

        private const val MAX_VALID_COORDINATE = 1_000_000f
        private const val MIN_VELOCITY_FOR_HEADING = 0.5f
        private const val MPS_TO_KMH = 3.6f
    }
}
