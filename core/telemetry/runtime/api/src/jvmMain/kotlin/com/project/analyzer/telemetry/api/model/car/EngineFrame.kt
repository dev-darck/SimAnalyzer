package com.project.analyzer.telemetry.api.model.car

public data class EngineFrame(
    val gear: Int? = null, // 0=reverse, 1=neutral, 2+=forward gears
    val rpm: Int? = null,
    val maxRpm: Int? = null,
    val currentMaxRpm: Float? = null, // Limiter-adjusted max RPM
    val maxGears: Int? = null,
    val engineType: String? = null,

    val turboBoost: Float? = null, // bar
    val turboBoostLevel: Float? = null,
    val turboBoostPercent: Float? = null,

    // ERS/KERS (not used in ACC, but available in AC)
    val kersCharge: Float? = null, // 0..1
    val kersInput: Float? = null, // 0..1
    val kersCurrentKJ: Float? = null,

    // Engine state
    val ignitionOn: Boolean? = null,
    val starterEngineOn: Boolean? = null,
    val isEngineRunning: Boolean? = null,
    val isRpmLimiterOn: Boolean? = null,
    val isChangeUpRpm: Boolean? = null,
    val isChangeDownRpm: Boolean? = null,
    val kersIsCharging: Boolean? = null,
    val batteryIsCharging: Boolean? = null,
    val maxKjPerLapReached: Boolean? = null,
    val maxChargeKjPerLapReached: Boolean? = null,

    // Temperature
    val waterTempC: Float? = null, // Coolant temperature °C
    val waterTempPercent: Float? = null,
    val exhaustTempC: Float? = null, // Exhaust temperature °C
    val oilTempC: Float? = null,
    val waterPressureBar: Float? = null,
    val oilPressureBar: Float? = null,
    val fuelPressureBar: Float? = null,
    val currentTorqueNm: Float? = null,
    val currentPowerHp: Int? = null,
    val batteryTempC: Float? = null,
    val batteryVoltage: Float? = null,

    // Engine braking
    val engineBrake: Int? = null, // Engine brake level

    // Auto shifter
    val autoShifterOn: Boolean? = null,
    val gearRpmWindow: Float? = null,
    val performanceModeName: String? = null,
)
