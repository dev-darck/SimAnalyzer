package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.math.Vec2
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

internal class RespawnResetDetector {

    private var hasPrev = false
    private var prevX = 0f
    private var prevZ = 0f

    private var lastResetNs = 0L
    private var lastResumeNs = 0L
    private var lastSeenPacketId = -1

    fun reset() {
        hasPrev = false
        prevX = 0f
        prevZ = 0f
        lastResetNs = 0L
        lastResumeNs = 0L
        lastSeenPacketId = -1
    }

    fun onResumed(nowNs: Long, physicsPacketId: Int) {
        lastResumeNs = nowNs
        lastSeenPacketId = physicsPacketId
        hasPrev = false
    }

    fun update(
        nowNs: Long,
        physicsPacketId: Int,
        speedKmh: Float,
        tyreContactPoint: FloatArray,
        position: Vec2,
    ): Boolean {
        if (physicsPacketId == lastSeenPacketId) return false
        lastSeenPacketId = physicsPacketId

        val hasContact =
            tyreContactPoint.size >= 6 &&
                (
                    abs(
                        tyreContactPoint[0],
                    ) + abs(tyreContactPoint[2]) + abs(tyreContactPoint[3]) + abs(tyreContactPoint[5])
                    ) > TYRE_CONTACT_EPSILON
        if (!hasContact) {
            hasPrev = false
            return false
        }

        if (!hasPrev) {
            hasPrev = true
            prevX = position.x
            prevZ = position.y
            return false
        }

        val dx = position.x - prevX
        val dz = position.y - prevZ
        val dist2 = dx * dx + dz * dz

        prevX = position.x
        prevZ = position.y

        val teleported = dist2 > TELEPORT_DISTANCE2_THRESHOLD && speedKmh < TELEPORT_SPEED_KMH_THRESHOLD
        val cooldownOk = (nowNs - lastResetNs) > RESET_COOLDOWN_NS
        val resumeOk = (nowNs - lastResumeNs) > RESET_COOLDOWN_NS

        if (teleported && cooldownOk && resumeOk) {
            lastResetNs = nowNs
            hasPrev = false
            return true
        }
        return false
    }

    private companion object {

        const val TYRE_CONTACT_EPSILON = 0.001f
        const val TELEPORT_SPEED_KMH_THRESHOLD = 5f
        const val TELEPORT_DISTANCE_THRESHOLD = 80f
        const val TELEPORT_DISTANCE2_THRESHOLD = TELEPORT_DISTANCE_THRESHOLD * TELEPORT_DISTANCE_THRESHOLD
        val RESET_COOLDOWN_NS = 2.seconds.inWholeNanoseconds
    }
}
