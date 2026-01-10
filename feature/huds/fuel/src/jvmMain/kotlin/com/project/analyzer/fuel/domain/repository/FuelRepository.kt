package com.project.analyzer.fuel.domain.repository

import com.project.analyzer.fuel.data.model.SavedFuelData

interface FuelRepository {

    suspend fun updateIfBetter(
        carModel: String,
        trackId: String,
        peakLitersPerLap: Double?,
        bestValidLapTimeMs: Int?
    )

    suspend fun load(carModel: String, trackId: String): SavedFuelData?

    suspend fun clear(carModel: String, trackId: String)
}
