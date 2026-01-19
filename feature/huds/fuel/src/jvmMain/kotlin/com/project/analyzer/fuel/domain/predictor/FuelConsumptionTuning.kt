package com.project.analyzer.fuel.domain.predictor

data class FuelConsumptionTuning(
    /** Time to collect data before showing estimates (seconds) */
    val warmupDurationSec: Double = 15.0,

    /** Minimum speed to consider car "moving" (km/h) */
    val movingMinSpeedKmh: Double = 5.0,

    /** Minimum speed to exit PIT_WAITING phase (km/h) */
    val pitExitSpeedKmh: Double = 20.0,

    /** Minimum fuel burned to start showing estimates (liters) */
    val minFuelBurnedForEstimate: Double = 0.001,

    /** EWMA time constant for fuel rate smoothing (seconds) */
    val tauFuelRateSec: Double = 8.0,

    /** EWMA time constant for lap time estimation from speed (seconds) */
    val tauLapTimeSec: Double = 6.0,

    /** EWMA time constant for speed smoothing (seconds) */
    val tauSpeedSec: Double = 5.0,

    /** Fallback lap time if nothing else available (seconds) */
    val fallbackLapTimeSec: Double = 90.0,

    /** Minimum plausible lap time (seconds) */
    val minLapTimeSec: Double = 30.0,

    /** Maximum plausible lap time (seconds) */
    val maxLapTimeSec: Double = 900.0,

    /** Minimum track length for speed-based lap time estimation (meters) */
    val minTrackLengthMeters: Double = 500.0,

    /** Minimum plausible fuel in tank (liters) */
    val minPlausibleFuelLiters: Double = 0.05,

    /** Maximum plausible fuel in tank (liters) */
    val maxPlausibleFuelLiters: Double = 300.0,

    /** Maximum plausible fuel consumption rate (liters/second) */
    val maxPlausibleRateLps: Double = 0.25,

    /** Maximum plausible liters per lap */
    val maxPlausibleLitersPerLap: Double = 50.0,

    /** Threshold for detecting refuel (liters increase) */
    val refuelThresholdLiters: Double = 0.5,

    /** Maximum delta time to accept (seconds) - skip frame if exceeded */
    val dtMaxSec: Double = 0.5,

    /** Minimum fuel consumed in a lap to count it (liters) */
    val minFuelConsumedPerLap: Double = 0.001,

    /** EWMA alpha for per-lap fuel averaging */
    val lapFuelEwmaAlpha: Double = 0.35,

    /** Minimum valid laps required for high confidence per-lap mode */
    val minValidLapsForPerLap: Int = 1,

    // Confidence thresholds
    val predictiveConfMin: Double = 0.3,
    val predictiveConfMax: Double = 0.7,
    val perLapConf2Plus: Double = 0.95,
)
