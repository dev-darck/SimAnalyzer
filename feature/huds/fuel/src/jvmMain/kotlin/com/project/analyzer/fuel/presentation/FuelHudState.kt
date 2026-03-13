package com.project.analyzer.fuel.presentation

import com.project.analyzer.fuel.domain.model.FuelPhase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class FuelHudUiState(
    val isShow: Boolean = false,
    val isSessionActive: Boolean = false,
    val phase: FuelPhase = FuelPhase.PIT_WAITING,
    val mainValue: String = "—",
    val peakValue: String = "—",

    val fuelLeftText: String = "—",
    val lapsRemainingCount: Int? = null,
    val lapsRemainingIsApprox: Boolean = true,
    val lastLapTimeText: String = "",
    val lapBasisText: String = "—",
    val displayLapNumber: Int = 1,

    val planRows: ImmutableList<PlanRowUi> = persistentListOf(),

    val lapBasisIsApprox: Boolean = true,
    val fuelLeftLitersRaw: Double? = null,
    val litersPerLapRaw: Double? = null,
    val peakLitersPerLapRaw: Double? = null,
    val planFuelLitersRaw: ImmutableList<Double?> = persistentListOf(),
    val isCurrentLapValid: Boolean = true,

    val confidence: Double = 0.0,
)

data class PlanRowUi(val laps: Int, val timeText: String, val fuelText: String, val peakFuelText: String = "—")
