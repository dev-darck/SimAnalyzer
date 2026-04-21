package com.project.analyzer.telemetry.api.model.car

public data class FuelFrame(
    val fuelLiters: Float? = null, // Current fuel
    val maxFuelLiters: Float? = null, // Tank capacity
    val fuelPercent: Float? = null,

    val fuelPerLapLiters: Float? = null, // Average fuel consumption per lap
    val fuelUsedLiters: Float? = null, // Total fuel used in session
    val fuelEstimatedLaps: Float? = null, // Laps remaining on current fuel
    val fuelPerKmLiters: Float? = null,
    val kmPerLiter: Float? = null,
    val instantaneousFuelPerKmLiters: Float? = null,
    val instantaneousKmPerLiter: Float? = null,

    // MFD settings for pit stop
    val mfdFuelToAdd: Float? = null, // Fuel to add at pit stop (liters)
)
