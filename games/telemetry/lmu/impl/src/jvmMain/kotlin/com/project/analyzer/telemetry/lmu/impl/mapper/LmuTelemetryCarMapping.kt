package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.ControlsFrame
import com.project.analyzer.telemetry.api.model.car.EngineFrame
import com.project.analyzer.telemetry.api.model.car.FuelFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleTelemetry
import kotlin.math.sqrt

internal fun mapCar(telemetry: LmuVehicleTelemetry): CarFrame {
    val localVel = telemetry.localVel
    val wheels = telemetry.wheels
    val frontRideHeight = wheels.takeIf { it.size >= 2 }
        ?.let { ((it[0].rideHeight + it[1].rideHeight) / 2.0).toFloat() }
    val rearRideHeight = wheels.takeIf { it.size >= 4 }
        ?.let { ((it[2].rideHeight + it[3].rideHeight) / 2.0).toFloat() }

    return CarFrame(
        controls = ControlsFrame(
            throttle = telemetry.filteredThrottle.toFloat(),
            brake = telemetry.filteredBrake.toFloat(),
            clutch = telemetry.filteredClutch.toFloat(),
            steerAngle = telemetry.filteredSteering.toFloat(),
            brakeBias = telemetry.rearBrakeBias.toFloat(),
        ),
        engine = EngineFrame(
            gear = telemetry.gear,
            rpm = telemetry.engineRpm.toInt(),
            maxRpm = telemetry.engineMaxRpm.toInt(),
            turboBoost = telemetry.turboBoostPressure.toFloat(),
            waterTempC = telemetry.engineWaterTemp.toFloat(),
            kersCharge = telemetry.batteryChargeFraction.toFloat().takeIf { it >= 0f },
            isEngineRunning = telemetry.engineRpm > 100.0,
        ),
        fuel = FuelFrame(
            fuelLiters = telemetry.fuel.toFloat(),
            maxFuelLiters = telemetry.fuelCapacity.toFloat(),
        ),
        speedKmh = speedKmh(localVel),
        localVelocity = localVel,
        worldPosition = telemetry.pos,
        accelerationG = telemetry.localAccel,
        localAngularVelocity = telemetry.localRot,
        rideHeightFront = frontRideHeight,
        rideHeightRear = rearRideHeight,
    )
}

private fun speedKmh(velocity: Vec3): Float {
    val speedMs = sqrt(
        velocity.x * velocity.x +
            velocity.y * velocity.y +
            velocity.z * velocity.z,
    )
    return speedMs * MS_TO_KMH
}

private const val MS_TO_KMH = 3.6f
private const val G_FORCE_MS2 = 9.80665
