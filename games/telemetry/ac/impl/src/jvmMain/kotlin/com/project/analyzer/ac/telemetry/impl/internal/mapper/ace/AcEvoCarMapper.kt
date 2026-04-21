package com.project.analyzer.ac.telemetry.impl.internal.mapper.ace

import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.api.model.car.AssistsFrame
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.ControlsFrame
import com.project.analyzer.telemetry.api.model.car.EngineFrame
import com.project.analyzer.telemetry.api.model.car.FuelFrame
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt

@Inject
internal class AcEvoCarMapper {

    fun enrich(base: CarFrame?, snapshot: AceRawSnapshot): CarFrame {
        val physics = snapshot.physics
        val graphics = snapshot.graphics
        val baseCar = base ?: CarFrame()

        return baseCar.copy(
            controls = enrichControls(baseCar.controls, graphics),
            engine = enrichEngine(baseCar.engine, physics, graphics),
            fuel = enrichFuel(baseCar.fuel, graphics),
            assists = enrichAssists(baseCar.assists, physics, graphics),
            speedKmh = physics.speedKmh.takeIf { it >= 0f } ?: baseCar.speedKmh,
            localVelocity = Vec3(physics.localVelocity[0], physics.localVelocity[1], physics.localVelocity[2]),
            accelerationG = Vec3(graphics.gForcesX, graphics.gForcesY, graphics.gForcesZ),
            finalFF = graphics.ffbStrength.takeIf { it.isFinite() } ?: baseCar.finalFF,
            lightsStage = graphics.instrumentation.mainLightStage.toInt(),
            rainLightsOn = graphics.instrumentation.rainLights,
            flashingLightsOn = graphics.instrumentation.flashingLights,
            directionLightsLeft = graphics.instrumentation.directionLightLeft,
            directionLightsRight = graphics.instrumentation.directionLightRight,
            displaySpeedKmh = graphics.displaySpeedKmh,
            displaySpeedMph = graphics.displaySpeedMph,
            displaySpeedMs = graphics.displaySpeedMs,
            steeringDegrees = graphics.steerDegrees,
            odometerKm = graphics.totalKm,
            drivingTimeSec = graphics.totalDrivingTimeS,
            ffMultiplier = graphics.carFfbMultiplier.takeIf { it.isFinite() } ?: baseCar.ffMultiplier,
            carLocation = graphics.carLocation?.name ?: baseCar.carLocation,
            isWrongWay = graphics.isWrongWay,
            specialLightsStage = graphics.instrumentation.specialLightStage.toInt(),
            cockpitLightStage = graphics.instrumentation.cockpitLightStage.toInt(),
            warningLightsOn = graphics.instrumentation.warningLights,
            areHeadlightsVisible = graphics.instrumentation.areHeadlightsVisible,
            displayPageIndices = graphics.instrumentation.displayCurrentPageIndex.toList(),
            performanceMeter = physics.performanceMeter.takeIf { it.isFinite() } ?: baseCar.performanceMeter,
        )
    }

    private fun enrichControls(
        base: ControlsFrame?,
        graphics: com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView,
    ): ControlsFrame = (base ?: ControlsFrame()).copy(
        throttle = graphics.gasPercent,
        brake = graphics.brakePercent,
        clutch = graphics.clutchPercent,
        handbrake = graphics.handbrakePercent,
        steeringInput = graphics.steeringPercent,
        brakeBias = graphics.electronics.brakeBias,
        pitspeedingDelta = graphics.pitspeedingDelta.takeIf { it.isFinite() } ?: base?.pitspeedingDelta,
    )

    private fun enrichEngine(
        base: EngineFrame?,
        physics: SPageFilePhysics,
        graphics: com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView,
    ): EngineFrame {
        val currentMaxRpm = physics.currentMaxRPM.takeIf { it.isFinite() && it > 0f }

        return (base ?: EngineFrame()).copy(
            gear = graphics.gearInt,
            rpm = graphics.rpm,
            maxRpm = currentMaxRpm?.roundToInt() ?: base?.maxRpm,
            currentMaxRpm = currentMaxRpm ?: base?.currentMaxRpm,
            maxGears = graphics.maxGears.toInt().takeIf { it > 0 } ?: base?.maxGears,
            engineType = graphics.engineType?.name ?: base?.engineType,
            turboBoost = graphics.turboBoost.takeIf { it.isFinite() } ?: base?.turboBoost,
            turboBoostLevel = graphics.turboBoostLevel.takeIf { it.isFinite() } ?: base?.turboBoostLevel,
            turboBoostPercent = graphics.turboBoostPerc.takeIf { it.isFinite() } ?: base?.turboBoostPercent,
            ignitionOn = graphics.isIgnitionOn,
            isEngineRunning = graphics.isEngineRunning,
            isRpmLimiterOn = graphics.isRpmLimiterOn,
            isChangeUpRpm = graphics.isChangeUpRpm,
            isChangeDownRpm = graphics.isChangeDownRpm,
            kersIsCharging = graphics.kersIsCharging,
            batteryIsCharging = graphics.batteryIsCharging,
            maxKjPerLapReached = graphics.isMaxKjPerLapReached,
            maxChargeKjPerLapReached = graphics.isMaxChargeKjPerLapReached,
            waterTempC = graphics.waterTemperatureC.toFloat(),
            waterTempPercent = graphics.waterTemperaturePercent.takeIf { it.isFinite() } ?: base?.waterTempPercent,
            exhaustTempC = graphics.exhaustTemperatureC,
            oilTempC = graphics.oilTemperatureC.takeIf { it > 0f },
            waterPressureBar = graphics.waterPressureBar.takeIf { it > 0f },
            oilPressureBar = graphics.oilPressureBar.takeIf { it > 0f },
            fuelPressureBar = graphics.fuelPressureBar.takeIf { it > 0f },
            currentTorqueNm = graphics.currentTorque.takeIf { it.isFinite() },
            currentPowerHp = graphics.currentBhp.takeIf { it > 0 },
            batteryTempC = graphics.batteryTemperature.takeIf { it > 0f },
            batteryVoltage = graphics.batteryVoltage.takeIf { it > 0f },
            gearRpmWindow = graphics.gearRpmWindow.takeIf { it.isFinite() } ?: base?.gearRpmWindow,
            performanceModeName = graphics.performanceModeName.ifBlank { base?.performanceModeName },
        )
    }

    private fun enrichFuel(
        base: FuelFrame?,
        graphics: com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView,
    ): FuelFrame = (base ?: FuelFrame()).copy(
        fuelLiters = graphics.fuelLiterCurrentQuantity.takeIf { it >= 0f } ?: base?.fuelLiters,
        maxFuelLiters = graphics.maxFuel.takeIf { it > 0f } ?: base?.maxFuelLiters,
        fuelPercent = graphics.fuelLiterCurrentQuantityPercent.takeIf { it >= 0f },
        fuelPerLapLiters = firstPositive(graphics.fuelPerLap, graphics.fuelLiterPerLap) ?: base?.fuelPerLapLiters,
        fuelUsedLiters = graphics.fuelLiterUsed.takeIf { it >= 0f } ?: base?.fuelUsedLiters,
        fuelEstimatedLaps = firstPositive(
            graphics.fuelEstimatedLaps,
            graphics.lapsPossibleWithFuel,
        ) ?: base?.fuelEstimatedLaps,
        fuelPerKmLiters = graphics.fuelLiterPerKm.takeIf { it >= 0f },
        kmPerLiter = graphics.kmPerFuelLiter.takeIf { it >= 0f },
        instantaneousFuelPerKmLiters = graphics.instantaneousFuelLiterPerKm.takeIf { it >= 0f },
        instantaneousKmPerLiter = graphics.instantaneousKmPerFuelLiter.takeIf { it >= 0f },
    )

    private fun enrichAssists(
        base: AssistsFrame?,
        physics: SPageFilePhysics,
        graphics: com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView,
    ): AssistsFrame = (base ?: AssistsFrame()).copy(
        tcLevel = graphics.electronics.tcLevel.toInt(),
        tcCut = graphics.electronics.tcCutLevel.toInt(),
        tcActive = graphics.tcActive,
        absLevel = graphics.electronics.absLevel.toInt(),
        absActive = graphics.absActive,
        escActive = graphics.escActive,
        launchActive = graphics.launchActive,
        engineMap = graphics.electronics.engineMapLevel.toInt(),
        ebbLevel = graphics.electronics.ebbLevel.toInt(),
        turboLevel = graphics.electronics.turboLevel,
        ersDeploymentMap = graphics.electronics.ersDeploymentMap.toInt(),
        ersRechargeMap = graphics.electronics.ersRechargeMap,
        diffPowerLevel = graphics.electronics.diffPowerLevel.toInt(),
        diffCoastLevel = graphics.electronics.diffCoastLevel.toInt(),
        diffPowerValue = graphics.diffPowerRawValue,
        diffCoastValue = graphics.diffCoastRawValue,
        frontBumpDamperLevel = graphics.electronics.frontBumpDamperLevel.toInt(),
        frontReboundDamperLevel = graphics.electronics.frontReboundDamperLevel.toInt(),
        rearBumpDamperLevel = graphics.electronics.rearBumpDamperLevel.toInt(),
        rearReboundDamperLevel = graphics.electronics.rearReboundDamperLevel.toInt(),
        activePerformanceMode = graphics.electronics.activePerformanceMode.toInt(),
        p2pActivations = physics.P2PActivations.takeIf { it >= 0 } ?: base?.p2pActivations,
        p2pStatus = physics.P2PStatus.takeIf { it >= 0 } ?: base?.p2pStatus,
        drsAvailable = graphics.isDrsAvailable,
        drsEnabled = graphics.electronics.isDrsOpen,
        pitLimiterOn = graphics.electronics.isPitLimiterOn,
        autoPitLimiter = graphics.assistsState.autoPitLimiter > 0,
        idealLineOn = graphics.assistsState.autoSteer > 0f,
        autoGear = graphics.assistsState.autoGear > 0,
        autoBlip = graphics.assistsState.autoBlip > 0,
        autoClutch = graphics.assistsState.autoClutch > 0,
        autoClutchOnStart = graphics.assistsState.autoClutchOnStart > 0,
        manualIgnitionStarter = graphics.assistsState.manualIgnitionEStart > 0,
        standingStartAssist = graphics.assistsState.standingStartAssist > 0,
        autoSteer = graphics.assistsState.autoSteer,
        stabilityControl = graphics.assistsState.arcadeStabilityControl,
        ersHeatChargingOn = graphics.electronics.isErsHeatChargingOn,
        ersOvertakeModeOn = graphics.electronics.isErsOvertakeModeOn,
        wiperLevel = graphics.instrumentation.wiperLevel.toInt(),
    )

    private fun firstPositive(primary: Float, fallback: Float): Float? = when {
        primary.isFinite() && primary > 0f -> primary
        fallback.isFinite() && fallback > 0f -> fallback
        else -> null
    }
}
