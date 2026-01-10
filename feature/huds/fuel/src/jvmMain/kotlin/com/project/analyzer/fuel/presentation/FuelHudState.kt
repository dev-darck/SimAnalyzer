package com.project.analyzer.fuel.presentation

import com.project.analyzer.fuel.domain.model.FuelPhase

data class FuelHudUiState(
    val isShow: Boolean = false,
    val isSessionActive: Boolean = false,
    val phase: FuelPhase = FuelPhase.PIT_WAITING,

    val title: String = "Fuel / lap",
    val mainValue: String = "—",
    val peakValue: String = "—",
    val subtitle: String = "Waiting in pits…",

    val fuelLeftText: String = "—",
    val lapsRemainingText: String = "—",
    val lastLapTimeText: String = "",
    val lapBasisText: String = "—",

    val planRows: List<PlanRowUi> = emptyList(),

    val lapBasisIsApprox: Boolean = true,
    val fuelLeftLitersRaw: Double? = null,
    val litersPerLapRaw: Double? = null,
    val peakLitersPerLapRaw: Double? = null,
    val planFuelLitersRaw: List<Double?> = emptyList(),
    val isCurrentLapValid: Boolean = true,

    val confidence: Double = 0.0
)

data class PlanRowUi(
    val label: String,
    val timeText: String,
    val fuelText: String,
    val peakFuelText: String = "—"
)
