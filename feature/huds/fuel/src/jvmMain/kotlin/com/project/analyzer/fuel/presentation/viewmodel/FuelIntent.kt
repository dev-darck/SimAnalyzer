package com.project.analyzer.fuel.presentation.viewmodel

sealed interface FuelIntent {
    data object Start : FuelIntent
    data object ResetAll : FuelIntent
}
