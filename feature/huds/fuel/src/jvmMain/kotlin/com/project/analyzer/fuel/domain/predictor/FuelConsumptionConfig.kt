package com.project.analyzer.fuel.domain.predictor

data object FuelConsumptionConfig {

    /** Laps to show in plan: 5, 10, 15 laps */
    val planLaps: List<Int> = listOf(5, 10, 15)

    /** Default times (minutes) to show before we have real lap time */
    val planMinutes: List<Int> = listOf(5, 10, 15)

    /** Safety margin for fuel calculation (percent) */
    const val safetyMarginPercent: Double = 0.0

    /** Tuning parameters */
    val tuning: FuelConsumptionTuning = FuelConsumptionTuning()
}
