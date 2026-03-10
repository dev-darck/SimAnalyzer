package com.project.analyzer.telemetry.ac.api.calibration

import com.project.analyzer.math.Heading2D
import com.project.analyzer.math.Pose2D
import com.project.analyzer.math.Vec2
import com.project.analyzer.math.Vec3
import com.project.analyzer.math.toVec2XZIfValid
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import kotlin.math.sqrt

public class ReferencePointPoseExtractor(private val referencePoint: ReferencePoint) {

    private var hasPreviousFrame = false
    private var previousPosition = Vec2.Zero
    private var previousForward = Vec2.Up
    private var previousTimestampNs = 0L

    public fun extract(frame: TelemetryFrame, timestampNs: Long = System.nanoTime()): Pose2D? {
        val wheelPositions = extractWheelPositions(frame)
        val fallbackPosition = frame.car?.worldPosition?.toVec2IfValid()
        val position = when {
            wheelPositions != null -> computeReferencePosition(wheelPositions)
            fallbackPosition != null -> fallbackPosition
            else -> return null
        }
        val forward = computeForwardDirection(frame, wheelPositions)

        if (hasPreviousFrame && isTeleport(position, timestampNs)) {
            hasPreviousFrame = false
        }

        hasPreviousFrame = true
        previousPosition = position
        previousForward = forward
        previousTimestampNs = timestampNs

        return Pose2D(position, forward)
    }

    private fun extractWheelPositions(frame: TelemetryFrame): WheelPositions? {
        val wheels = frame.wheels ?: return null
        val fl = wheels.fl?.contactPoint?.toVec2IfValid()
        val fr = wheels.fr?.contactPoint?.toVec2IfValid()
        val rl = wheels.rl?.contactPoint?.toVec2IfValid()
        val rr = wheels.rr?.contactPoint?.toVec2IfValid()
        if (!hasWheelPosition(fl, fr, rl, rr)) return null

        return WheelPositions(
            fl = fl,
            fr = fr,
            rl = rl,
            rr = rr,
        )
    }

    private fun computeReferencePosition(wheelPositions: WheelPositions): Vec2 {
        val fallback = wheelPositions.carCenter ?: Vec2.Zero
        return when (referencePoint) {
            ReferencePoint.FRONT_AXLE -> wheelPositions.frontAxleCenter ?: fallback
            ReferencePoint.REAR_AXLE -> wheelPositions.rearAxleCenter ?: fallback
            ReferencePoint.CAR_CENTER -> wheelPositions.carCenter ?: fallback
        }
    }

    private fun computeForwardDirection(frame: TelemetryFrame, wheelPositions: WheelPositions?): Vec2 {
        val velocityDirection = frame.car?.velocity?.let(::resolveVelocityDirection)
        val axleDirection = wheelPositions?.resolveAxleDirection()
        val headingDirection = frame.car?.heading?.let { heading ->
            Heading2D.resolveFromMirroredHeadingRad(
                headingRad = heading,
                prefer = velocityDirection ?: axleDirection,
            )
        }

        return velocityDirection ?: axleDirection ?: headingDirection ?: previousForward
    }

    private fun resolveVelocityDirection(velocity: Vec3): Vec2? {
        val speed = sqrt(velocity.x * velocity.x + velocity.z * velocity.z)
        if (speed <= MIN_VELOCITY_FOR_DIRECTION) return null

        return Vec2(
            x = velocity.x / speed,
            y = velocity.z / speed,
        )
    }

    private fun isTeleport(currentPosition: Vec2, currentTimestampNs: Long): Boolean {
        if (!hasPreviousFrame) return false

        val distance = (currentPosition - previousPosition).len()
        val deltaSeconds = (currentTimestampNs - previousTimestampNs) / NS_PER_SECOND
        if (distance > TELEPORT_DISTANCE_THRESHOLD) return true
        if (deltaSeconds <= 0.001f) return false

        val speedKmh = (distance / deltaSeconds) * 3.6f
        return speedKmh > MAX_REALISTIC_SPEED_KMH
    }

    private fun hasWheelPosition(vararg points: Vec2?): Boolean = points.any { it != null }

    private fun Vec3.toVec2IfValid(): Vec2? = toVec2XZIfValid(maxAbsCoordinate = MAX_VALID_COORDINATE)

    private data class WheelPositions(val fl: Vec2?, val fr: Vec2?, val rl: Vec2?, val rr: Vec2?) {

        val frontAxleCenter: Vec2? = axleCenter(fl, fr)
        val rearAxleCenter: Vec2? = axleCenter(rl, rr)
        val carCenter: Vec2? = when {
            frontAxleCenter != null && rearAxleCenter != null -> frontAxleCenter.midpoint(rearAxleCenter)
            frontAxleCenter != null -> frontAxleCenter
            else -> rearAxleCenter
        }

        fun resolveAxleDirection(): Vec2? {
            val front = frontAxleCenter ?: return null
            val rear = rearAxleCenter ?: return null
            val delta = front - rear
            val length = delta.len()
            return if (length in 1.0f..6.0f) delta * (1f / length) else null
        }

        private fun axleCenter(first: Vec2?, second: Vec2?): Vec2? = when {
            first != null && second != null -> first.midpoint(second)
            first != null -> first
            else -> second
        }
    }

    private companion object {

        const val MAX_VALID_COORDINATE = 1_000_000f
        const val MIN_VELOCITY_FOR_DIRECTION = 0.5f
        const val TELEPORT_DISTANCE_THRESHOLD = 50f
        const val MAX_REALISTIC_SPEED_KMH = 500f
        const val NS_PER_SECOND = 1_000_000_000f
    }
}
