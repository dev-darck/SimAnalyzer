package com.project.analyzer.calibration.data

import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.calibration.data.model.ReferencePoint
import com.project.analyzer.calibration.data.model.Vec2
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import com.project.analyzer.telemetry.ac.api.model.math.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PoseExtractor(
    private val referencePoint: ReferencePoint
) {

    private var hasPrev = false
    private var prevPos = Vec2(0f, 0f)
    private var prevForward = Vec2(1f, 0f)

    fun reset() {
        hasPrev = false
        prevPos = Vec2(0f, 0f)
        prevForward = Vec2(1f, 0f)
    }

    fun extract(frame: TelemetryFrame): Pose2D? {
        val fl = frame.wheels?.fl?.contactPoint
        val fr = frame.wheels?.fr?.contactPoint
        val rl = frame.wheels?.rl?.contactPoint
        val rr = frame.wheels?.rr?.contactPoint

        fun valid(p: Vec3?) = p != null && (p.x != 0f || p.z != 0f) && abs(p.x) < 1e6f && abs(p.z) < 1e6f
        fun toV2(p: Vec3) = Vec2(p.x, p.z)

        fun axleCenter(a: Vec3?, b: Vec3?): Vec2? {
            val aa = a?.takeIf(::valid)
            val bb = b?.takeIf(::valid)
            return when {
                aa != null && bb != null -> (toV2(aa) + toV2(bb)) * 0.5f
                aa != null -> toV2(aa)
                bb != null -> toV2(bb)
                else -> null
            }
        }

        val front = axleCenter(fl, fr)
        val rear = axleCenter(rl, rr)
        val center = when {
            front != null && rear != null -> (front + rear) * 0.5f
            front != null -> front
            rear != null -> rear
            else -> null
        } ?: return null

        val posCandidate: Vec2 = when (referencePoint) {
            ReferencePoint.FRONT_AXLE -> front ?: center
            ReferencePoint.REAR_AXLE -> rear ?: center
            ReferencePoint.CAR_CENTER -> center
        }

        val forwardAxle = if (front != null && rear != null) {
            val v = front - rear
            if (v.length() >= 0.05f) v.normalized() else null
        } else {
            null
        }

        val heading = frame.car?.heading
        val forwardHeading = if (heading != null) Vec2(sin(heading), cos(heading)).normalized() else null

        if (forwardAxle != null && forwardHeading != null) {
            val dot = forwardAxle.dot(forwardHeading)
            if (dot < 0.95f) {
                println("[PoseExtractor] forwardAxle=$forwardAxle forwardHeading=$forwardHeading dot=$dot heading=$heading")
            }
        }


        val pos = if (!hasPrev) {
            posCandidate
        } else {
            val delta = posCandidate - prevPos
            val dist = delta.length()

            val looksLikeTeleport = dist > 25f

            if (!looksLikeTeleport) {
                posCandidate
            } else {
                val fallbackDist = (center - prevPos).length()
                if (fallbackDist <= 25f) center else prevPos
            }
        }

        val forward = forwardAxle ?: forwardHeading ?: prevForward

        hasPrev = true
        prevPos = pos
        prevForward = forward

        return Pose2D(pos, forward)
    }
}
