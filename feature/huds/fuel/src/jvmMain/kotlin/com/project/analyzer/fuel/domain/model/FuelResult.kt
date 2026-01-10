package com.project.analyzer.fuel.domain.model

/**
 * Sealed result from UseCase
 */
sealed interface FuelResult {

    data object SessionEnded : FuelResult
    data object NoData : FuelResult
    data object Reset : FuelResult
    data class Data(val estimate: FuelEstimate) : FuelResult
}
