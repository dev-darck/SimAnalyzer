package com.project.analyzer.ac.telemetry.impl.fallback.pose

import com.project.analyzer.ac.telemetry.impl.fallback.pose.model.CarPose
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.math.Heading2D
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint

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

        val frontCenter = fl.midpoint(fr)
        val rearCenter = rl.midpoint(rr)

        val pos = when (referencePoint) {
            ReferencePoint.FRONT_AXLE -> frontCenter
            ReferencePoint.REAR_AXLE -> rearCenter
            ReferencePoint.CAR_CENTER -> frontCenter.midpoint(rearCenter)
        }

        val headingRad = physics.heading
        val headingDirA = Heading2D.forwardFromRad(headingRad)

        val axleForward = run {
            val d = frontCenter - rearCenter
            val l = d.len()
            if (l in 1.0f..6.0f) d * (1f / l) else null
        }

        val v = physics.velocity
        val localVx = v.getOrNull(0) ?: 0f
        val localVz = v.getOrNull(2) ?: 0f

        val worldVel = Heading2D.localToWorldXZ(localVx, localVz, headingRad)
        val velocityDir = worldVel.safeNormalized(headingDirA)

        val headingDir = Heading2D.resolveBidirectional(
            a = headingDirA,
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
}
