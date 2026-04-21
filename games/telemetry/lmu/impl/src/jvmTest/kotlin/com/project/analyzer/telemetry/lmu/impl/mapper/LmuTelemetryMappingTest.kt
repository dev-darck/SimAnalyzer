package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleTelemetry
import kotlin.test.Test
import kotlin.test.assertEquals

internal class LmuTelemetryMappingTest {

    @Test
    fun carMappingConvertsRf2GearEncodingToCommonEncoding() {
        assertEquals(0, mapCar(telemetry(gear = -1)).engine?.gear)
        assertEquals(1, mapCar(telemetry(gear = 0)).engine?.gear)
        assertEquals(2, mapCar(telemetry(gear = 1)).engine?.gear)
        assertEquals(3, mapCar(telemetry(gear = 2)).engine?.gear)
    }

    @Test
    fun lapMappingUsesElapsedTimeWhenLapStartIsZero() {
        val lap = mapLap(
            telemetry = telemetry(
                elapsedTime = 12.345,
                lapNumber = 0,
                lapStartEt = 0.0,
            ),
            scoring = null,
        )

        assertEquals(1, lap.currentLapIndex)
        assertEquals(0, lap.completedLaps)
        assertEquals(12_345, lap.currentLapTimeMs)
    }

    @Test
    fun lapMappingUsesElapsedMinusLapStartAfterLapStarted() {
        val lap = mapLap(
            telemetry = telemetry(
                elapsedTime = 462.04,
                lapNumber = 1,
                lapStartEt = 404.49,
            ),
            scoring = null,
        )

        assertEquals(2, lap.currentLapIndex)
        assertEquals(1, lap.completedLaps)
        assertEquals(57_550, lap.currentLapTimeMs)
    }

    private fun telemetry(
        elapsedTime: Double = 0.0,
        lapNumber: Int = 0,
        lapStartEt: Double = 0.0,
        gear: Int = 0,
    ): LmuVehicleTelemetry {
        val zero = Vec3(0f, 0f, 0f)
        return LmuVehicleTelemetry(
            id = 0,
            deltaTime = 0.0,
            elapsedTime = elapsedTime,
            lapNumber = lapNumber,
            lapStartEt = lapStartEt,
            vehicleName = "",
            trackName = "",
            pos = zero,
            localVel = zero,
            localAccel = zero,
            ori = emptyList(),
            localRot = zero,
            localRotAccel = zero,
            gear = gear,
            engineRpm = 0.0,
            engineWaterTemp = 0.0,
            engineOilTemp = 0.0,
            clutchRpm = 0.0,
            unfilteredThrottle = 0.0,
            unfilteredBrake = 0.0,
            unfilteredSteering = 0.0,
            unfilteredClutch = 0.0,
            filteredThrottle = 0.0,
            filteredBrake = 0.0,
            filteredSteering = 0.0,
            filteredClutch = 0.0,
            steeringShaftTorque = 0.0,
            front3rdDeflection = 0.0,
            rear3rdDeflection = 0.0,
            frontWingHeight = 0.0,
            frontRideHeight = 0.0,
            rearRideHeight = 0.0,
            drag = 0.0,
            frontDownforce = 0.0,
            rearDownforce = 0.0,
            fuel = 0.0,
            engineMaxRpm = 0.0,
            scheduledStops = 0,
            overheating = false,
            detached = false,
            headlights = false,
            dentSeverity = ByteArray(8),
            lastImpactEt = 0.0,
            lastImpactMagnitude = 0.0,
            lastImpactPos = zero,
            engineTorque = 0.0,
            currentSector = 0,
            speedLimiter = 0,
            maxGears = 0,
            frontTireCompoundIndex = 0,
            rearTireCompoundIndex = 0,
            fuelCapacity = 0.0,
            frontFlapActivated = 0,
            rearFlapActivated = 0,
            rearFlapLegalStatus = 0,
            ignitionStarter = 0,
            frontTireCompoundName = "",
            rearTireCompoundName = "",
            speedLimiterAvailable = 0,
            antiStallActivated = 0,
            unused = ByteArray(2),
            visualSteeringWheelRange = 0f,
            rearBrakeBias = 0.0,
            turboBoostPressure = 0.0,
            physicsToGraphicsOffset = FloatArray(3),
            physicalSteeringWheelRange = 0f,
            deltaBest = 0.0,
            batteryChargeFraction = 0.0,
            electricBoostMotorTorque = 0.0,
            electricBoostMotorRpm = 0.0,
            electricBoostMotorTemperature = 0.0,
            electricBoostWaterTemperature = 0.0,
            electricBoostMotorState = 0,
            lapInvalidated = false,
            absActive = false,
            tcActive = false,
            speedLimiterActive = false,
            wiperState = 0,
            tc = 0,
            tcMax = 0,
            tcSlip = 0,
            tcSlipMax = 0,
            tcCut = 0,
            tcCutMax = 0,
            abs = 0,
            absMax = 0,
            motorMap = 0,
            motorMapMax = 0,
            migration = 0,
            migrationMax = 0,
            frontAntiSway = 0,
            frontAntiSwayMax = 0,
            rearAntiSway = 0,
            rearAntiSwayMax = 0,
            liftAndCoastProgress = 0,
            trackLimitsSteps = 0,
            regen = 0f,
            stateOfCharge = 0f,
            virtualEnergy = 0f,
            timeGapCarAhead = 0f,
            timeGapCarBehind = 0f,
            timeGapPlaceAhead = 0f,
            timeGapPlaceBehind = 0f,
            vehicleModel = "",
            vehicleClass = 0,
            vehicleChampionship = 0,
            expansion = ByteArray(20),
            wheels = emptyList(),
        )
    }
}
