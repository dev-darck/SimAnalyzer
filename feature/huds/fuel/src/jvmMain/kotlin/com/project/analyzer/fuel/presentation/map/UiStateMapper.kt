package com.project.analyzer.fuel.presentation.map

import com.project.analyzer.fuel.domain.model.FuelEstimate
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionConfig
import com.project.analyzer.fuel.presentation.FuelHudUiState
import com.project.analyzer.fuel.presentation.PlanRowUi
import java.util.Locale
import kotlin.math.roundToInt

internal fun FuelEstimate.toUiState(
    config: FuelConsumptionConfig,
    safetyFactor: Double
): FuelHudUiState {
    val lpl = litersPerLap
    val lapTimeSec = estimatedLapTimeSec ?: config.tuning.fallbackLapTimeSec
    val hasRealLapTime = isLapTimeFromCompletedLap

    val subtitle = when (phase) {
        FuelPhase.PIT_WAITING -> "Waiting in pits…"

        FuelPhase.WARMUP -> {
            "Collecting data… "
        }

        FuelPhase.PREDICTIVE -> {
            val confPct = (confidence * 100).roundToInt()
            "Predictive • $confPct%"
        }

        FuelPhase.PER_LAP -> {
            val confPct = (confidence * 100).roundToInt()
            "Per-lap ($completedLaps) • $confPct%"
        }
    }

    val mainValue = lpl?.let { formatLiters(it, 2) } ?: "—"

    val fuelLeftText = formatLiters(currentFuelLiters, 2)

    val lapsRemainingText = lapsRemaining?.let {
        if (isLapTimeFromCompletedLap) "~${it.roundToInt()} laps"
        else "≈${it.roundToInt()} laps"
    } ?: "—"

    val lastLapTimeText = lastLapTimeMs?.let { formatLapTime(it) } ?: ""

    val lapBasisText = if (hasRealLapTime) {
        formatLapTimeFromSec(lapTimeSec)
    } else {
        "≈${formatLapTimeFromSec(lapTimeSec)}"
    }

    val planRows = buildPlanRows(
        config = config,
        litersPerLap = lpl,
        lapTimeSec = lapTimeSec,
        hasRealLapTime = hasRealLapTime,
        safetyFactor = safetyFactor
    )

    val planFuelLitersRaw = planRows.map { row ->
        row.fuelText.replace(" L", "").replace("—", "").toDoubleOrNull()
    }

    return FuelHudUiState(
        isShow = true,
        isSessionActive = true,
        phase = phase,
        title = "Fuel / lap",
        mainValue = mainValue,
        peakValue = "—",
        subtitle = subtitle,
        fuelLeftText = fuelLeftText,
        lapsRemainingText = lapsRemainingText,
        lastLapTimeText = lastLapTimeText,
        lapBasisText = lapBasisText,
        planRows = planRows,
        lapBasisIsApprox = !hasRealLapTime,
        fuelLeftLitersRaw = currentFuelLiters,
        litersPerLapRaw = lpl,
        planFuelLitersRaw = planFuelLitersRaw,
        confidence = confidence,
        isCurrentLapValid = isCurrentLapValid
    )
}

private fun buildPlanRows(
    config: FuelConsumptionConfig,
    litersPerLap: Double?,
    lapTimeSec: Double,
    hasRealLapTime: Boolean,
    safetyFactor: Double
): List<PlanRowUi> {
    val rows = mutableListOf<PlanRowUi>()

    for (laps in config.planLaps) {
        val totalTimeSec = laps * lapTimeSec
        val timeText = if (hasRealLapTime) {
            formatDuration(totalTimeSec)
        } else {
            "≈${formatDuration(totalTimeSec)}"
        }

        val fuelNeeded = litersPerLap?.let { it * laps * safetyFactor }
        val fuelText = fuelNeeded?.let { "${it.roundToInt()} L" } ?: "—"

        rows.add(
            PlanRowUi(
                label = "$laps laps",
                timeText = timeText,
                fuelText = fuelText,
                peakFuelText = "—"
            )
        )
    }

    return rows
}

private fun formatLiters(liters: Double, decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f L", liters)
}

private fun formatLapTime(ms: Int): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val millis = ms % 1000
    return String.format(Locale.US, "%d:%02d.%03d", minutes, seconds, millis)
}

private fun formatLapTimeFromSec(sec: Double): String {
    val totalMs = (sec * 1000).roundToInt()
    return formatLapTime(totalMs)
}

private fun formatDuration(totalSec: Double): String {
    val minutes = (totalSec / 60).toInt()
    val seconds = (totalSec % 60).toInt()
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
