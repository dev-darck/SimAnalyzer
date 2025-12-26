package com.project.analyzer.telemetry.ac.api.model.car

public data class FuelFrame(
    val fuelLiters: Float? = null, // Current fuel
    val maxFuelLiters: Float? = null, // Tank capacity

    val fuelPerLapLiters: Float? = null, // Average fuel consumption per lap
    val fuelUsedLiters: Float? = null, // Total fuel used in session
    val fuelEstimatedLaps: Float? = null, // Laps remaining on current fuel

    // MFD settings for pit stop
    val mfdFuelToAdd: Float? = null, // Fuel to add at pit stop (liters)
)
