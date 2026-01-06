package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.structure.toBoolean
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.ac.api.model.car.AssistsFrame
import com.project.analyzer.telemetry.ac.api.model.car.CarFrame
import com.project.analyzer.telemetry.ac.api.model.car.ControlsFrame
import com.project.analyzer.telemetry.ac.api.model.car.EngineFrame
import com.project.analyzer.telemetry.ac.api.model.car.FuelFrame
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class CarMapper {

    fun map(
        physics: SPageFilePhysics,
        graphics: SPageFileGraphics,
        statics: SPageFileStatic
    ): CarFrame {
        return CarFrame(
            controls = mapControls(physics),
            engine = mapEngine(physics, graphics, statics),
            fuel = mapFuel(physics, graphics, statics),
            assists = mapAssists(physics, graphics),

            speedKmh = physics.speedKmh,
            velocity = physics.velocity.toVec3(),
            localVelocity = physics.localVelocity.toVec3(),
            accelerationG = physics.accG.toVec3(),

            heading = physics.heading,
            pitch = physics.pitch,
            roll = physics.roll,

            localAngularVelocity = physics.localAngularVel.toVec3(),

            cgHeight = physics.cgHeight.takeIf { it != 0f },
            rideHeightFront = physics.rideHeight[0],
            rideHeightRear = physics.rideHeight[1],

            finalFF = physics.finalFF,

            kerbVibration = physics.kerbVibration,
            slipVibrations = physics.slipVibrations,
            gVibrations = physics.gVibrations,
            absVibrations = physics.absVibrations,

            lightsStage = graphics.lightsStage,
            rainLightsOn = graphics.rainLights.toBoolean(),
            flashingLightsOn = graphics.flashingLights.toBoolean(),
            directionLightsLeft = graphics.directionLightsLeft.toBoolean(),
            directionLightsRight = graphics.directionLightsRight.toBoolean(),
        )
    }

    private fun mapControls(physics: SPageFilePhysics): ControlsFrame {
        return ControlsFrame(
            throttle = physics.gas,
            brake = physics.brake,
            clutch = physics.clutch,
            steerAngle = physics.steerAngle,
            brakeBias = physics.brakeBias,
            brakePressureFL = physics.brakePressure[0],
            brakePressureFR = physics.brakePressure[1],
            brakePressureRL = physics.brakePressure[2],
            brakePressureRR = physics.brakePressure[3],
        )
    }

    private fun mapEngine(
        physics: SPageFilePhysics,
        graphics: SPageFileGraphics,
        statics: SPageFileStatic
    ): EngineFrame {
        return EngineFrame(
            gear = physics.gear,
            rpm = physics.rpm,
            maxRpm = statics.maxRpm,
            currentMaxRpm = physics.currentMaxRPM,

            turboBoost = physics.turboBoost.takeIf { it > 0 },

            kersCharge = physics.kersCharge.takeIf { it > 0 },
            kersInput = physics.kersInput.takeIf { it > 0 },
            kersCurrentKJ = physics.kersCurrentKJ.takeIf { it > 0 },

            ignitionOn = physics.ignitionOn.toBoolean(),
            starterEngineOn = physics.starterEngineOn.toBoolean(),
            isEngineRunning = physics.isEngineRunning.toBoolean(),

            waterTempC = physics.waterTemp,
            exhaustTempC = graphics.exhaustTemperature,

            engineBrake = physics.engineBrake,
            autoShifterOn = physics.autoShifterOn.toBoolean(),
        )
    }

    private fun mapFuel(
        physics: SPageFilePhysics,
        graphics: SPageFileGraphics,
        statics: SPageFileStatic
    ): FuelFrame {
        return FuelFrame(
            fuelLiters = physics.fuel,
            maxFuelLiters = statics.maxFuel,
            fuelPerLapLiters = graphics.fuelXLap.takeIf { it > 0 },
            fuelUsedLiters = graphics.usedFuel,
            fuelEstimatedLaps = graphics.fuelEstimatedLaps.takeIf { it > 0 },
            mfdFuelToAdd = graphics.mfdFuelToAdd,
        )
    }

    private fun mapAssists(
        physics: SPageFilePhysics,
        graphics: SPageFileGraphics
    ): AssistsFrame {
        return AssistsFrame(
            tcLevel = graphics.tc,
            tcCut = graphics.tcCut,
            tcValue = physics.tc,
            tcInAction = physics.tcInAction.toBoolean(),

            absLevel = graphics.abs,
            absValue = physics.abs,
            absInAction = physics.absInAction.toBoolean(),

            engineMap = graphics.engineMap,

            drsAvailable = physics.drsAvailable.toBoolean(),
            drsEnabled = physics.drsEnabled.toBoolean(),

            pitLimiterOn = physics.pitLimiterOn.toBoolean(),
            idealLineOn = graphics.idealLineOn.toBoolean(),
            wiperLevel = graphics.wiperLV,
        )
    }
}

private fun FloatArray.toVec3(): Vec3 = Vec3(this[0], this[1], this[2])
