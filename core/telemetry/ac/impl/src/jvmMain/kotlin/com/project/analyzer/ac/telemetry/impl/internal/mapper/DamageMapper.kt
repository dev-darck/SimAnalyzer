package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.model.car.damage.AeroDamageFrame
import com.project.analyzer.telemetry.ac.api.model.car.damage.BodyDamageFrame
import com.project.analyzer.telemetry.ac.api.model.car.damage.DamageFrame
import com.project.analyzer.telemetry.ac.api.model.car.damage.SuspensionDamageFrame
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class DamageMapper {

    fun map(physics: SPageFilePhysics): DamageFrame {
        return DamageFrame(
            rawDamage5 = physics.carDamage.copyOf(),

            aero = AeroDamageFrame(
                frontWing = physics.carDamage[DAMAGE_FRONT],
                rearWing = physics.carDamage[DAMAGE_REAR],
            ),
            suspension = SuspensionDamageFrame(
                fl = physics.suspensionDamage[0],
                fr = physics.suspensionDamage[1],
                rl = physics.suspensionDamage[2],
                rr = physics.suspensionDamage[3],
            ),
            body = BodyDamageFrame(
                left = physics.carDamage[DAMAGE_LEFT],
                right = physics.carDamage[DAMAGE_RIGHT],
                centre = physics.carDamage[DAMAGE_CENTRE],
            ),

            numberOfTyresOut = physics.numberOfTyresOut,
        )
    }

    private companion object {

        const val DAMAGE_FRONT = 0
        const val DAMAGE_REAR = 1
        const val DAMAGE_LEFT = 2
        const val DAMAGE_RIGHT = 3
        const val DAMAGE_CENTRE = 4
    }
}
