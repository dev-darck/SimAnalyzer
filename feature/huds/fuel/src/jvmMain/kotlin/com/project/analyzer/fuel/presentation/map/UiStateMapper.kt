package com.project.analyzer.fuel.presentation.map

import com.project.analyzer.fuel.domain.model.FuelEstimate
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionConfig
import com.project.analyzer.fuel.presentation.FuelHudUiState
import com.project.analyzer.fuel.presentation.PlanRowUi
import com.project.analyzer.utils.ext.fromMsToLapTime
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

internal fun FuelEstimate.toUiState(safetyFactor: Double): FuelHudUiState {
    val lpl = litersPerLap
    val lapTimeSec = estimatedLapTimeSec ?: FuelConsumptionConfig.tuning.fallbackLapTimeSec

    return FuelHudUiState(
        isShow = true,
        isSessionActive = true,
        phase = phase,
        mainValue = lpl?.formatLiters(2) ?: "—",
        peakValue = "—",
        fuelLeftText = currentFuelLiters.formatLiters(2),
        lapsRemainingCount = displayLapsRemainingCount(),
        lapsRemainingIsApprox = !isLapTimeFromCompletedLap,
        lastLapTimeText = lastLapTimeMs?.fromMsToLapTime().orEmpty(),
        lapBasisText = formatLapBasis(lapTimeSec),
        planRows = buildPlanRows(lpl, lapTimeSec, safetyFactor),
        displayLapNumber = displayLapNumber(),
        lapBasisIsApprox = !isLapTimeFromCompletedLap,
        fuelLeftLitersRaw = currentFuelLiters,
        litersPerLapRaw = lpl,
        planFuelLitersRaw = FuelConsumptionConfig.planLaps.map { laps ->
            lpl?.let { it * laps * safetyFactor }
        },
        confidence = confidence,
        isCurrentLapValid = isCurrentLapValid,
    )
}

private fun FuelEstimate.displayLapNumber(): Int =
    maxOf(
        currentLapIndex?.takeIf { it > 0 } ?: 0,
        (completedLaps + 1).coerceAtLeast(1),
    )

private fun FuelEstimate.displayLapsRemainingCount(): Int? {
    val laps = lapsRemaining?.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: return null
    return if (phase == FuelPhase.PER_LAP && gameFuelEstimatedLaps != null) {
        ceil(laps).toInt()
    } else {
        laps.roundToInt()
    }
}

private fun FuelEstimate.formatLapBasis(lapTimeSec: Double): String {
    val formatted = (lapTimeSec * 1000).roundToInt().fromMsToLapTime()
    return if (isLapTimeFromCompletedLap) formatted else "≈$formatted"
}

private fun buildPlanRows(litersPerLap: Double?, lapTimeSec: Double, safetyFactor: Double): List<PlanRowUi> =
    FuelConsumptionConfig.planLaps.map { laps ->
        val totalTimeSec = laps * lapTimeSec
        val fuelNeeded = litersPerLap?.let { it * laps * safetyFactor }

        PlanRowUi(
            laps = laps,
            timeText = formatDuration(totalTimeSec),
            fuelText = fuelNeeded?.let { "${it.roundToInt()} L" } ?: "—",
            peakFuelText = "—",
        )
    }

private fun Double.formatLiters(decimals: Int): String = String.format(Locale.US, "%.${decimals}f L", this)

private fun formatDuration(totalSec: Double): String {
    val minutes = (totalSec / 60).toInt()
    val seconds = (totalSec % 60).toInt()
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
