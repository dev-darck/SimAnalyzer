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
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class CarMapper {

    private val logger = logger()

    fun map(physics: SPageFilePhysics, graphics: SPageFileGraphics, statics: SPageFileStatic): CarFrame = CarFrame(
        controls = mapControls(physics),
        engine = mapEngine(physics, graphics, statics),
        fuel = mapFuel(physics, graphics, statics),
        assists = mapAssists(physics, graphics),

        speedKmh = physics.speedKmh.sanitize(0f, MAX_SPEED_KMH),
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
        throttle = physics.gas.sanitize(0f, 1f),
        brake = physics.brake.sanitize(0f, 1f),
        clutch = physics.clutch.sanitize(0f, 1f),
        steerAngle = physics.steerAngle,
        brakeBias = physics.brakeBias.sanitize(0f, 1f),
        brakePressureFL = physics.brakePressure[0],
        brakePressureFR = physics.brakePressure[1],
        brakePressureRL = physics.brakePressure[2],
        brakePressureRR = physics.brakePressure[3],
    )

    private fun mapEngine(
        physics: SPageFilePhysics,
        graphics: SPageFileGraphics,
        statics: SPageFileStatic,
    ): EngineFrame {
        val rawRpm = physics.rpm
        val rawMaxRpm = statics.maxRpm
        val rawCurrentMaxRpm = physics.currentMaxRPM

        val rpm = rawRpm.sanitize(0, MAX_RPM)
        val maxRpm = rawMaxRpm.sanitize(0, MAX_RPM)
        val currentMaxRpm = rawCurrentMaxRpm.sanitize(0f, MAX_RPM.toFloat())

        if (rawRpm != rpm || rawMaxRpm != maxRpm) {
            logger.atWarn(RATE_LIMITED) {
                message = "RPM sanity clamped: rpm=$rawRpm→$rpm maxRpm=$rawMaxRpm→$maxRpm " +
                    "currentMaxRpm=$rawCurrentMaxRpm→$currentMaxRpm"
            }
        }

        return EngineFrame(
            gear = physics.gear.sanitize(-1, MAX_GEAR),
            rpm = rpm,
            maxRpm = maxRpm,
            currentMaxRpm = currentMaxRpm,

            turboBoost = physics.turboBoost.takeIf { it in 0f..MAX_TURBO_BOOST },

            kersCharge = physics.kersCharge.takeIf { it in 0f..1f },
            kersInput = physics.kersInput.takeIf { it in 0f..1f },
            kersCurrentKJ = physics.kersCurrentKJ.takeIf { it in 0f..MAX_KERS_KJ },

            ignitionOn = physics.ignitionOn.toBoolean(),
            starterEngineOn = physics.starterEngineOn.toBoolean(),
            isEngineRunning = physics.isEngineRunning.toBoolean(),

            waterTempC = physics.waterTemp.sanitize(MIN_TEMP_C, MAX_TEMP_C),
            exhaustTempC = graphics.exhaustTemperature.sanitize(MIN_TEMP_C, MAX_EXHAUST_TEMP_C),

            engineBrake = physics.engineBrake,
            autoShifterOn = physics.autoShifterOn.toBoolean(),
        )
    }

    private fun mapFuel(physics: SPageFilePhysics, graphics: SPageFileGraphics, statics: SPageFileStatic): FuelFrame =
        FuelFrame(
            fuelLiters = physics.fuel.sanitize(0f, MAX_FUEL_LITERS),
            maxFuelLiters = statics.maxFuel.sanitize(0f, MAX_FUEL_LITERS),
            fuelPerLapLiters = graphics.fuelXLap.takeIf { it in 0f..MAX_FUEL_LITERS },
            fuelUsedLiters = graphics.usedFuel.sanitize(0f, MAX_FUEL_LITERS),
            fuelEstimatedLaps = graphics.fuelEstimatedLaps.takeIf { it in 0f..MAX_FUEL_ESTIMATED_LAPS },
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

    private companion object {

        const val MAX_RPM = 25_000 // F1 engines peak ~15k; 25k is generous
        const val MAX_GEAR = 12 // Most cars have ≤8 gears
        const val MAX_SPEED_KMH = 500f // Fastest road cars ~450 km/h
        const val MAX_TURBO_BOOST = 10f // Bar
        const val MAX_KERS_KJ = 10_000f
        const val MAX_FUEL_LITERS = 500f
        const val MAX_FUEL_ESTIMATED_LAPS = 999f
        const val MIN_TEMP_C = -50f
        const val MAX_TEMP_C = 200f // Water temp
        const val MAX_EXHAUST_TEMP_C = 1500f

        fun Int.sanitize(min: Int, max: Int): Int =
            if (this in min..max) this else min

        fun Float.sanitize(min: Float, max: Float): Float =
            if (this.isFinite() && this in min..max) this else min
    }
}

private fun FloatArray.toVec3(): Vec3 = Vec3(this[0], this[1], this[2])
