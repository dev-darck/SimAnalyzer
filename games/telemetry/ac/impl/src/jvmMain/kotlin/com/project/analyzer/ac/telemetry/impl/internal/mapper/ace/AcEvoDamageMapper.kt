package com.project.analyzer.ac.telemetry.impl.internal.mapper.ace

import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.telemetry.api.model.car.damage.AeroDamageFrame
import com.project.analyzer.telemetry.api.model.car.damage.BodyDamageFrame
import com.project.analyzer.telemetry.api.model.car.damage.DamageFrame
import com.project.analyzer.telemetry.api.model.car.damage.SuspensionDamageFrame
import dev.zacsweers.metro.Inject

@Inject
internal class AcEvoDamageMapper {

    fun enrich(base: DamageFrame?, snapshot: AceRawSnapshot): DamageFrame {
        val damage = snapshot.graphics.carDamage
        return (base ?: DamageFrame()).copy(
            rawDamage5 = floatArrayOf(
                damage.damageFront,
                damage.damageRear,
                damage.damageLeft,
                damage.damageRight,
                damage.damageCenter,
            ),
            aero = AeroDamageFrame(
                frontWing = damage.damageFront,
                rearWing = damage.damageRear,
            ),
            body = BodyDamageFrame(
                left = damage.damageLeft,
                right = damage.damageRight,
                centre = damage.damageCenter,
            ),
            suspension = SuspensionDamageFrame(
                fl = damage.damageSuspensionLf,
                fr = damage.damageSuspensionRf,
                rl = damage.damageSuspensionLr,
                rr = damage.damageSuspensionRr,
            ),
        )
    }
}
