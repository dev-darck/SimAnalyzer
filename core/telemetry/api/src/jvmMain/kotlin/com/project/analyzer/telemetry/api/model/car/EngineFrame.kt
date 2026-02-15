package com.project.analyzer.telemetry.api.model.car

public data class EngineFrame(
    val gear: Int? = null, // 0=reverse, 1=neutral, 2+=forward gears
    val rpm: Int? = null,
    val maxRpm: Int? = null,
    val currentMaxRpm: Float? = null, // Limiter-adjusted max RPM

    val turboBoost: Float? = null, // bar

    // ERS/KERS (not used in ACC, but available in AC)
    val kersCharge: Float? = null, // 0..1
    val kersInput: Float? = null, // 0..1
    val kersCurrentKJ: Float? = null,

    // Engine state
    val ignitionOn: Boolean? = null,
    val starterEngineOn: Boolean? = null,
    val isEngineRunning: Boolean? = null,

    // Temperature
    val waterTempC: Float? = null, // Coolant temperature °C
    val exhaustTempC: Float? = null, // Exhaust temperature °C

    // Engine braking
    val engineBrake: Int? = null, // Engine brake level

    // Auto shifter
    val autoShifterOn: Boolean? = null,
)
