package com.project.analyzer.fuel.domain.repository

import com.project.analyzer.fuel.data.model.SavedFuelData

interface FuelRepository {

    suspend fun updateIfBetter(carId: Int, trackId: String, peakLitersPerLap: Double?, bestValidLapTimeMs: Int?)

    suspend fun load(carId: Int, trackId: String): SavedFuelData?

    suspend fun clear(carId: Int, trackId: String)
}
