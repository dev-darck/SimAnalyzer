package com.project.analyzer.fuel.data.model

data class SavedFuelData(
    val peakLitersPerLap: Double?,
    val bestValidLapTimeMs: Int?,
    val savedAtEpochMs: Long
)
