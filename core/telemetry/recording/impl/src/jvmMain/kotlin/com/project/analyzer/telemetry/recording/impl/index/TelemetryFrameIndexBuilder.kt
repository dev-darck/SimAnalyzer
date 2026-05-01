package com.project.analyzer.telemetry.recording.impl.index

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndex
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndexFlags
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingFrameSnapshot
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.atan2
import kotlin.math.sqrt

@Inject
@SingleIn(SessionScope::class)
class TelemetryFrameIndexBuilder {

    fun build(frame: TelemetryRecordingFrameSnapshot): TelemetryFrameIndex {
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

    private fun extractPosition(frame: TelemetryRecordingFrameSnapshot): Vec2? {
        coordinate(frame.car?.worldPositionX, frame.car?.worldPositionZ)?.let { return it }

        val wheels = frame.wheels ?: return null
        val fl = coordinate(wheels.fl?.contactPointX, wheels.fl?.contactPointZ)
        val fr = coordinate(wheels.fr?.contactPointX, wheels.fr?.contactPointZ)
        val rl = coordinate(wheels.rl?.contactPointX, wheels.rl?.contactPointZ)
        val rr = coordinate(wheels.rr?.contactPointX, wheels.rr?.contactPointZ)

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

    private fun coordinate(x: Float?, z: Float?): Vec2? {
        if (x == null || z == null) return null
        if (!x.isFinite() || !z.isFinite()) return null
        if (x !in -MAX_VALID_COORDINATE..MAX_VALID_COORDINATE) return null
        if (z !in -MAX_VALID_COORDINATE..MAX_VALID_COORDINATE) return null
        return Vec2(x, z)
    }

    private fun axleCenter(a: Vec2?, b: Vec2?): Vec2? = when {
        a != null && b != null -> a.midpoint(b)
        a != null -> a
        b != null -> b
        else -> null
    }

    private fun resolveSpeedKmh(frame: TelemetryRecordingFrameSnapshot): Float? {
        val speed = frame.car?.speedKmh
        if (speed != null) return speed

        val velocityX = frame.car?.velocityX ?: return null
        val velocityZ = frame.car?.velocityZ ?: return null
        val metersPerSecond = sqrt(velocityX * velocityX + velocityZ * velocityZ)
        return if (metersPerSecond > 0f) metersPerSecond * MPS_TO_KMH else null
    }

    private fun resolveHeading(frame: TelemetryRecordingFrameSnapshot): Float? {
        val heading = frame.car?.heading
        if (heading != null) return heading

        val velocityX = frame.car?.velocityX ?: return null
        val velocityZ = frame.car?.velocityZ ?: return null
        val velocityMagnitude = sqrt(velocityX * velocityX + velocityZ * velocityZ)
        if (velocityMagnitude < MIN_VELOCITY_FOR_HEADING) return null
        return atan2(velocityZ, velocityX)
    }

    private fun resolveLap(frame: TelemetryRecordingFrameSnapshot): Int? {
        val lap = frame.lap?.currentLapIndex
        if (lap != null && lap > 0) return lap

        val completed = frame.session?.completedLaps ?: return null
        return if (completed >= 0) completed + 1 else null
    }

    private fun buildFlags(frame: TelemetryRecordingFrameSnapshot): Int {
        var flags = 0

        val pit = frame.session?.pit
        flags = applyPitFlags(flags, pit)

        if (frame.lap?.validity == LapValidity.INVALID) {
            flags = flags or TelemetryFrameIndexFlags.INVALID_LAP
        }

        return flags
    }

    private fun applyPitFlags(base: Int, pit: TelemetryRecordingFrameSnapshot.PitSnapshot?): Int {
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
