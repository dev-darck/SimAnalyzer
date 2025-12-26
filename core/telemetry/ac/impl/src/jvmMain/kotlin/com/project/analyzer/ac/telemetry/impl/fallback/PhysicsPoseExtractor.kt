package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.math.Vec2
import kotlin.math.cos
import kotlin.math.sin

class PhysicsPoseExtractor {

    fun extract(
        physics: SPageFilePhysics,
        referencePoint: ReferencePoint
    ): CarPose? {

        val tcp = physics.tyreContactPoint
        if (tcp.size < 12) return null

        val fl = Vec2(tcp[0], tcp[2])
        val fr = Vec2(tcp[3], tcp[5])
        val rl = Vec2(tcp[6], tcp[8])
        val rr = Vec2(tcp[9], tcp[11])

        val frontCenter = fl axleCenter fr
        val rearCenter = rl axleCenter rr

        val pos = when (referencePoint) {
            ReferencePoint.FRONT_AXLE -> frontCenter
            ReferencePoint.REAR_AXLE -> rearCenter
            ReferencePoint.CAR_CENTER -> (frontCenter + rearCenter) * 0.5f
        }

        val headingRad = physics.heading

        val headingDirA = Vec2(sin(headingRad), cos(headingRad))
            .safeNormalized(Vec2(0f, 1f))

        val axleForward = run {
            val d = frontCenter - rearCenter
            val l = d.len()
            if (l in 1.0f..6.0f) d * (1f / l) else null
        }

        val v = physics.velocity
        val localVx = v.getOrNull(0) ?: 0f
        val localVz = v.getOrNull(2) ?: 0f

        val ch = cos(headingRad)
        val sh = sin(headingRad)

        val worldVx = localVx * ch + localVz * sh
        val worldVz = -localVx * sh + localVz * ch

        val worldVel = Vec2(worldVx, worldVz)
        val velocityDir = worldVel.safeNormalized(headingDirA)

        val headingDir = resolveHeadingDir(
            headingDirA = headingDirA,
            prefer = axleForward
        )

        val isMovingForward = if (worldVel.len() > 0.2f) {
            velocityDir.dot(headingDir) >= 0f
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

    private infix fun Vec2.axleCenter(b: Vec2): Vec2 = (this + b) * 0.5f

    private fun resolveHeadingDir(headingDirA: Vec2, prefer: Vec2?): Vec2 {
        val ref = prefer?.safeNormalized(Vec2(0f, 1f)) ?: return headingDirA
        val headingDirB = headingDirA * -1f
        return if (headingDirA.dot(ref) >= headingDirB.dot(ref)) headingDirA else headingDirB
    }
}
