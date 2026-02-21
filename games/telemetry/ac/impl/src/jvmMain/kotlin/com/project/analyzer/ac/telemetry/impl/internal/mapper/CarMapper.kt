package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.structure.toBoolean
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.api.model.car.AssistsFrame
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.ControlsFrame
import com.project.analyzer.telemetry.api.model.car.EngineFrame
import com.project.analyzer.telemetry.api.model.car.FuelFrame
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class CarMapper {

    fun map(physics: SPageFilePhysics, graphics: SPageFileGraphics, statics: SPageFileStatic): CarFrame = CarFrame(
        controls = mapControls(physics),
        engine = mapEngine(physics, graphics, statics),
        fuel = mapFuel(physics, graphics, statics),
        assists = mapAssists(physics, graphics),

        speedKmh = physics.speedKmh,
        velocity = physics.velocity.toVec3(),
        localVelocity = physics.localVelocity.toVec3(),
        accelerationG = physics.accG.toVec3(),
        worldPosition = resolveWorldPosition(graphics),

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

    private fun mapControls(physics: SPageFilePhysics): ControlsFrame = ControlsFrame(
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

    private fun mapEngine(
        physics: SPageFilePhysics,
        graphics: SPageFileGraphics,
        statics: SPageFileStatic,
    ): EngineFrame = EngineFrame(
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

    private fun mapFuel(physics: SPageFilePhysics, graphics: SPageFileGraphics, statics: SPageFileStatic): FuelFrame =
        FuelFrame(
            fuelLiters = physics.fuel,
            maxFuelLiters = statics.maxFuel,
            fuelPerLapLiters = graphics.fuelXLap.takeIf { it > 0 },
            fuelUsedLiters = graphics.usedFuel,
            fuelEstimatedLaps = graphics.fuelEstimatedLaps.takeIf { it > 0 },
            mfdFuelToAdd = graphics.mfdFuelToAdd,
        )

    private fun mapAssists(physics: SPageFilePhysics, graphics: SPageFileGraphics): AssistsFrame = AssistsFrame(
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

    private fun resolveWorldPosition(graphics: SPageFileGraphics): Vec3? {
        val carIds = graphics.carID
        val coords = graphics.carCoordinates
        val activeCount = graphics.activeCars.takeIf { it > 0 } ?: carIds.size
        val maxByCoords = coords.size / 3
        val limit = minOf(activeCount, carIds.size, maxByCoords)

        fun coordsAt(index: Int): Vec3? {
            if (index !in 0..<limit) return null
            val offset = index * 3
            if (offset + 2 >= coords.size) return null

            val x = coords[offset]
            val y = coords[offset + 1]
            val z = coords[offset + 2]
            if (x == 0f && y == 0f && z == 0f) return null
            if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
            return Vec3(x, y, z)
        }

        val playerId = graphics.playerCarID
        val playerIndex = carIds.indexOfFirst { it == playerId }
        val playerPos = coordsAt(playerIndex)
        if (playerPos != null) return playerPos

        for (i in 0 until limit) {
            val pos = coordsAt(i)
            if (pos != null) return pos
        }

        return null
    }
}

private fun FloatArray.toVec3(): Vec3 = Vec3(this[0], this[1], this[2])
