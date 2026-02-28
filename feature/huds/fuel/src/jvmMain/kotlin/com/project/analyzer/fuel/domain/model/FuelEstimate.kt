package com.project.analyzer.fuel.domain.model

/**
 * Complete fuel estimate from the engine
 */
data class FuelEstimate(
    val phase: FuelPhase,

    // Current fuel state
    val currentFuelLiters: Double,
    val maxFuelLiters: Double?,

    // Consumption rates
    val litersPerLap: Double?,
    val litersPerSecond: Double?,

    // Lap time info
    val lastLapTimeMs: Int?,
    val estimatedLapTimeSec: Double?,
    val isLapTimeFromCompletedLap: Boolean,

    // Laps remaining
    val lapsRemaining: Double?,

    // Game-provided data (if available)
    val gameFuelPerLap: Float?,
    val gameFuelEstimatedLaps: Float?,

    // Session info
    val carModel: String?,
    val carId: Int?,
    val trackId: String?,
    val currentLapIndex: Int?,
    val completedLaps: Int,

    // Data quality indicator (0..1)
    val confidence: Double,

    val isCurrentLapValid: Boolean = true,
)
