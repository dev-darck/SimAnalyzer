package com.project.analyzer.telemetry.ac.api.model.environment

public data class EnvironmentFrame(
    val airTempC: Float? = null,
    val roadTempC: Float? = null,
    val airDensity: Float? = null, // kg/m³

    val windSpeedMps: Float? = null, // m/s
    val windDirectionDeg: Float? = null, // degrees (0=North, 90=East)

    // Rain
    val rainIntensity: Float? = null, // 0..1 or enum value
    val rainIntensityIn10min: Int? = null,
    val rainIntensityIn30min: Int? = null,

    val surfaceGrip: Float? = null, // 0..1
    val trackGripStatus: Int? = null, // ACC_TRACK_GRIP_STATUS enum

    // Time of day
    val clockSeconds: Float? = null, // Seconds since midnight (in-game)
)
