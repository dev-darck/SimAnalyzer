package com.project.analyzer.fuel.domain.usecase

import com.project.analyzer.fuel.domain.model.FuelResult
import kotlinx.coroutines.flow.Flow

interface FuelConsumptionUseCase {

    val fuelEstimates: Flow<FuelResult>

    suspend fun resetAll()
}
