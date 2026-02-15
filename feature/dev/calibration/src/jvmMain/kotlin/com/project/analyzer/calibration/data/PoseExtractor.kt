package com.project.analyzer.calibration.data

import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.math.Heading2D
import com.project.analyzer.math.Vec2
import com.project.analyzer.math.Vec3
import com.project.analyzer.math.toVec2XZIfValid
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import kotlin.math.sqrt

class PoseExtractor(private val referencePoint: ReferencePoint) {

    private var hasPreviousFrame = false
    private var previousPosition = Vec2.Zero
    private var previousForward = Vec2.Up
    private var previousTimestampNs = 0L

    fun extract(frame: TelemetryFrame, timestampNs: Long = System.nanoTime()): Pose2D? {
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

    private data class WheelPositions(
        val fl: Vec2?,
        val fr: Vec2?,
        val rl: Vec2?,
        val rr: Vec2?,
    ) {

        val frontAxleCenter: Vec2? = axleCenter(fl, fr)
        val rearAxleCenter: Vec2? = axleCenter(rl, rr)
        val carCenter: Vec2? = run {
            val f = frontAxleCenter
            val r = rearAxleCenter
            when {
                f != null && r != null -> f.midpoint(r)
                f != null -> f
                r != null -> r
                else -> null
            }
        }

        private fun axleCenter(a: Vec2?, b: Vec2?): Vec2? = when {
            a != null && b != null -> a.midpoint(b)
            a != null -> a
            b != null -> b
            else -> null
        }
    }

    private fun extractWheelPositions(frame: TelemetryFrame): WheelPositions? {
        val w = frame.wheels ?: return null
        val fl = w.fl?.contactPoint?.toVec2IfValid()
        val fr = w.fr?.contactPoint?.toVec2IfValid()
        val rl = w.rl?.contactPoint?.toVec2IfValid()
        val rr = w.rr?.contactPoint?.toVec2IfValid()
        if (fl == null && fr == null && rl == null && rr == null) return null
        return WheelPositions(fl, fr, rl, rr)
    }

    private fun Vec3.toVec2IfValid(): Vec2? = toVec2XZIfValid(maxAbsCoordinate = MAX_VALID_COORDINATE)

    private fun computeReferencePosition(w: WheelPositions): Vec2 {
        val fallback = w.carCenter ?: Vec2.Zero
        return when (referencePoint) {
            ReferencePoint.FRONT_AXLE -> w.frontAxleCenter ?: fallback
            ReferencePoint.REAR_AXLE -> w.rearAxleCenter ?: fallback
            ReferencePoint.CAR_CENTER -> w.carCenter ?: fallback
        }
    }

    private fun computeForwardDirection(frame: TelemetryFrame, w: WheelPositions?): Vec2 {
        val vel = frame.car?.velocity
        val velocityDir: Vec2? = if (vel != null) {
            val vx = vel.x
            val vz = vel.z
            val sp = sqrt(vx * vx + vz * vz)
            if (sp > MIN_VELOCITY_FOR_DIRECTION) Vec2(vx / sp, vz / sp) else null
        } else null

        val axleDir: Vec2? = run {
            val f = w?.frontAxleCenter
            val r = w?.rearAxleCenter
            if (f != null && r != null) {
                val d = f - r
                val l = d.len()
                if (l in 1.0f..6.0f) d * (1f / l) else null
            } else null
        }

        val heading = frame.car?.heading
        val headingDir: Vec2? = if (heading != null) {
            Heading2D.resolveFromRadiansPoseExtractor(headingRad = heading, prefer = velocityDir ?: axleDir)
        } else null

        return velocityDir ?: axleDir ?: headingDir ?: previousForward
    }

    private fun isTeleport(currentPosition: Vec2, currentTimestampNs: Long): Boolean {
        if (!hasPreviousFrame) return false

        val dist = (currentPosition - previousPosition).len()
        val dtNs = currentTimestampNs - previousTimestampNs
        val dtSec = dtNs / NS_PER_SECOND

        if (dist > TELEPORT_DISTANCE_THRESHOLD) return true
        if (dtSec > 0.001f) {
            val speedKmh = (dist / dtSec) * 3.6f
            if (speedKmh > MAX_REALISTIC_SPEED_KMH) return true
        }
        return false
    }

    private companion object {

        const val MAX_VALID_COORDINATE = 1_000_000f
        const val MIN_VELOCITY_FOR_DIRECTION = 0.5f
        const val TELEPORT_DISTANCE_THRESHOLD = 50f
        const val MAX_REALISTIC_SPEED_KMH = 500f
        const val NS_PER_SECOND = 1_000_000_000f
    }
}
