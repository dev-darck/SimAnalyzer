package com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model

data class FuelSnapshot(
    val fuelPerLapLiters: Float? = null,
    val lastLapFuelPerLapLiters: Float? = null,
    val fuelPerLapEwmaLiters: Float? = null,
    val fuelEstimatedLaps: Float? = null,
)
