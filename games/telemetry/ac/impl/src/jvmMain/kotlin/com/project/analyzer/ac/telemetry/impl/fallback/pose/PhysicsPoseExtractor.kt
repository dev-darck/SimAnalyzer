package com.project.analyzer.ac.telemetry.impl.fallback.pose

import com.project.analyzer.ac.telemetry.impl.fallback.pose.model.CarPose
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.math.MathEps
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PhysicsPoseExtractor {

    fun extract(physics: SPageFilePhysics, referencePoint: ReferencePoint): CarPose? {
        val tcp = physics.tyreContactPoint
        if (tcp.size < 12) return null

        val frontCenterX = (tcp[0] + tcp[3]) * 0.5f
        val frontCenterY = (tcp[2] + tcp[5]) * 0.5f
        val rearCenterX = (tcp[6] + tcp[9]) * 0.5f
        val rearCenterY = (tcp[8] + tcp[11]) * 0.5f

        val pos = when (referencePoint) {
            ReferencePoint.FRONT_AXLE -> Vec2(frontCenterX, frontCenterY)

            ReferencePoint.REAR_AXLE -> Vec2(rearCenterX, rearCenterY)

            ReferencePoint.CAR_CENTER -> Vec2(
                x = (frontCenterX + rearCenterX) * 0.5f,
                y = (frontCenterY + rearCenterY) * 0.5f,
            )
        }

        val headingRad = physics.heading
        val headingDirA = safeNormalizedOrUp(
            x = sin(headingRad),
            y = cos(headingRad),
        )
        val headingAx = headingDirA.x
        val headingAy = headingDirA.y

        val axleForward = run {
            val dx = frontCenterX - rearCenterX
            val dy = frontCenterY - rearCenterY
            val len2 = dx * dx + dy * dy
            val len = sqrt(len2)
            if (len in 1.0f..6.0f) {
                val inv = 1f / len
                Vec2(dx * inv, dy * inv)
            } else {
                null
            }
        }

        val v = physics.localVelocity
        val localVx = v.getOrNull(0) ?: 0f
        val localVz = v.getOrNull(2) ?: 0f

        val ch = cos(headingRad)
        val sh = sin(headingRad)
        val worldVx = localVx * ch + localVz * sh
        val worldVy = -localVx * sh + localVz * ch

        val velocityDir = run {
            val len2 = worldVx * worldVx + worldVy * worldVy
            if (len2 > MathEps.EPS * MathEps.EPS) {
                val inv = 1f / sqrt(len2)
                Vec2(worldVx * inv, worldVy * inv)
            } else {
                headingDirA
            }
        }

        val headingDir = run {
            val prefer = axleForward
            if (prefer == null) {
                headingDirA
            } else {
                val preferLen2 = prefer.x * prefer.x + prefer.y * prefer.y
                val ref = if (preferLen2 > MathEps.EPS * MathEps.EPS) {
                    val inv = 1f / sqrt(preferLen2)
                    Vec2(prefer.x * inv, prefer.y * inv)
                } else {
                    Vec2.Up
                }
                val dot = headingAx * ref.x + headingAy * ref.y
                if (dot >= 0f) {
                    headingDirA
                } else {
                    Vec2(-headingAx, -headingAy)
                }
            }
        }

        val isMovingForward = if ((worldVx * worldVx + worldVy * worldVy) > (0.2f * 0.2f)) {
            (velocityDir.x * headingDir.x + velocityDir.y * headingDir.y) >= 0f
        } else {
            true
        }

        return CarPose(
            position = pos,
            velocityDir = velocityDir,
            headingDir = headingDir,
            speedKmh = physics.speedKmh,
            isMovingForward = isMovingForward,
        )
    }

    private fun safeNormalizedOrUp(x: Float, y: Float): Vec2 {
        val len2 = x * x + y * y
        if (len2 > MathEps.EPS * MathEps.EPS) {
            val inv = 1f / sqrt(len2)
            return Vec2(x * inv, y * inv)
        }
        return Vec2.Up
    }
}
