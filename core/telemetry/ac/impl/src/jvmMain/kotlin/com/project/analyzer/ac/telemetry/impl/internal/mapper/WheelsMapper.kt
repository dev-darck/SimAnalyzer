package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.toBoolean
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.ac.api.model.car.wheels.WheelFrame
import com.project.analyzer.telemetry.ac.api.model.car.wheels.WheelsFrame
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class WheelsMapper(
    private val cache: AcSessionCache
) {

    fun map(physics: SPageFilePhysics, graphics: SPageFileGraphics): WheelsFrame {
        return WheelsFrame(
            fl = mapWheel(physics, WHEEL_FL),
            fr = mapWheel(physics, WHEEL_FR),
            rl = mapWheel(physics, WHEEL_RL),
            rr = mapWheel(physics, WHEEL_RR),

            tyreCompound = cache.tyreCompound.takeIf { it.isNotBlank() },
            currentTyreSet = graphics.currentTyreSet,
            strategyTyreSet = graphics.strategyTyreSet,
            isRainTyres = graphics.rainTyres.toBoolean(),

            mfdPressureLF = graphics.mfdTyrePressureLF,
            mfdPressureRF = graphics.mfdTyrePressureRF,
            mfdPressureLR = graphics.mfdTyrePressureLR,
            mfdPressureRR = graphics.mfdTyrePressureRR,
            mfdTyreSet = graphics.mfdTyreSet,

            frontBrakeCompound = physics.frontBrakeCompound,
            rearBrakeCompound = physics.rearBrakeCompound,
        )
    }

    private fun mapWheel(physics: SPageFilePhysics, index: Int): WheelFrame {
        val contactOffset = index * 3

        return WheelFrame(
            pressurePsi = physics.wheelsPressure[index],
            wear = physics.tyreWear[index],
            dirtyLevel = physics.tyreDirtyLevel[index],

            coreTempC = physics.tyreCoreTemperature[index],
            innerTempC = physics.tyreTempI[index],
            middleTempC = physics.tyreTempM[index],
            outerTempC = physics.tyreTempO[index],
            avgTempC = physics.tyreTemp[index],

            brakeTempC = physics.brakeTemp[index],
            brakePressure = physics.brakePressure[index],
            padLife = physics.padLife[index],
            discLife = physics.discLife[index],

            slip = physics.wheelSlip[index],
//            slipRatio = physics.slipRatio[index],
//            slipAngle = physics.slipAngle[index],
            load = physics.wheelLoad[index].takeIf { it > 0 },
            angularSpeed = physics.wheelAngularSpeed[index],

            // Forces
            longitudinalForce = physics.fx[index],
            lateralForce = physics.fy[index],
            selfAligningTorque = physics.mz[index],

            // Suspension
            suspensionTravel = physics.suspensionTravel[index],
            camberRad = physics.camberRad[index],

            // Contact
            contactPoint = Vec3(
                physics.tyreContactPoint[contactOffset],
                physics.tyreContactPoint[contactOffset + 1],
                physics.tyreContactPoint[contactOffset + 2]
            ),
            contactNormal = Vec3(
                physics.tyreContactNormal[contactOffset],
                physics.tyreContactNormal[contactOffset + 1],
                physics.tyreContactNormal[contactOffset + 2]
            ),

            contactHeading = Vec3(
                physics.tyreContactHeading[contactOffset],
                physics.tyreContactHeading[contactOffset + 1],
                physics.tyreContactHeading[contactOffset + 2]
            )
        )
    }

    private companion object {

        const val WHEEL_FL = 0
        const val WHEEL_FR = 1
        const val WHEEL_RL = 2
        const val WHEEL_RR = 3
    }
}
