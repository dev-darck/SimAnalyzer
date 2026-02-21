package com.project.analyzer.fuel.presentation.map

import com.project.analyzer.fuel.domain.model.FuelEstimate
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionConfig
import com.project.analyzer.fuel.presentation.FuelHudUiState
import com.project.analyzer.fuel.presentation.PlanRowUi
import com.project.analyzer.utils.ext.fromMsToLapTime
import java.util.Locale
import kotlin.math.roundToInt

internal fun FuelEstimate.toUiState(safetyFactor: Double): FuelHudUiState {
    val lpl = litersPerLap
    val lapTimeSec = estimatedLapTimeSec ?: FuelConsumptionConfig.tuning.fallbackLapTimeSec

    return FuelHudUiState(
        isShow = true,
        isSessionActive = true,
        phase = phase,
        title = "Fuel / lap",
        mainValue = lpl?.formatLiters(2) ?: "—",
        peakValue = "—",
        subtitle = buildSubtitle(),
        fuelLeftText = currentFuelLiters.formatLiters(2),
        lapsRemainingText = formatLapsRemaining(),
        lastLapTimeText = lastLapTimeMs?.fromMsToLapTime().orEmpty(),
        lapBasisText = formatLapBasis(lapTimeSec),
        planRows = buildPlanRows(lpl, lapTimeSec, safetyFactor),
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

private fun FuelEstimate.buildSubtitle(): String = when (phase) {
    FuelPhase.PIT_WAITING -> "Waiting in pits…"
    FuelPhase.WARMUP -> "Collecting data…"
    FuelPhase.PREDICTIVE -> "Predictive • ${(confidence * 100).roundToInt()}%"
    FuelPhase.PER_LAP -> "Per-lap ($completedLaps) • ${(confidence * 100).roundToInt()}%"
}

private fun FuelEstimate.formatLapsRemaining(): String {
    val remaining = lapsRemaining ?: return "—"
    val prefix = if (isLapTimeFromCompletedLap) "~" else "≈"
    return "$prefix${remaining.roundToInt()} laps"
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
            label = "$laps laps",
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
