package com.analyzer.session.analysis.presentation.builder.coach

import com.project.analyzer.telemetry.analysis.api.model.handling.SessionAnalysisHandlingState
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun computeBalanceStats(
    selectedSamples: List<SessionAnalysisSample>,
    referenceSamples: List<SessionAnalysisSample>,
    strings: SessionAnalysisCoachStrings,
): BalanceStats {
    val selectedRatios = balanceRatios(selectedSamples)
    val referenceRatios = balanceRatios(referenceSamples)

    val dominant = when {
        selectedRatios.understeerRatio >= selectedRatios.oversteerRatio -> SessionAnalysisHandlingState.Understeer
        else -> SessionAnalysisHandlingState.Oversteer
    }
    val dominantRatio = when (dominant) {
        SessionAnalysisHandlingState.Understeer -> selectedRatios.understeerRatio
        SessionAnalysisHandlingState.Oversteer -> selectedRatios.oversteerRatio
        SessionAnalysisHandlingState.Neutral -> 0f
    }.takeIf { ratio -> ratio > 0f }
    val referenceRatio = when (dominant) {
        SessionAnalysisHandlingState.Understeer -> referenceRatios.understeerRatio
        SessionAnalysisHandlingState.Oversteer -> referenceRatios.oversteerRatio
        SessionAnalysisHandlingState.Neutral -> 0f
    }
    val deltaToReference = ((dominantRatio ?: 0f) - referenceRatio).coerceAtLeast(0f)

    val label = when {
        dominantRatio == null -> strings.balanceStable

        dominant == SessionAnalysisHandlingState.Understeer ->
            strings.understeerBalance((dominantRatio * 100f).roundToInt())

        else -> strings.oversteerBalance((dominantRatio * 100f).roundToInt())
    }
    val description = when {
        dominantRatio == null -> strings.balanceStableDescription

        dominant == SessionAnalysisHandlingState.Understeer ->
            strings.understeerBalanceDescription((dominantRatio * 100f).roundToInt())

        else -> strings.oversteerBalanceDescription((dominantRatio * 100f).roundToInt())
    }

    return BalanceStats(
        label = label,
        description = description,
        dominantRatio = dominantRatio,
        deltaToReference = deltaToReference,
    )
}

private fun balanceRatios(samples: List<SessionAnalysisSample>): BalanceRatios {
    val loadedSamples = samples.filter { sample ->
        (sample.speedKmh ?: 0f) >= COACH_LOADED_SPEED_KMH &&
            abs(sample.lateralG ?: 0f) >= COACH_LOADED_LATERAL_G
    }
    if (loadedSamples.isEmpty()) return BalanceRatios()

    val total = loadedSamples.size.toFloat()
    return BalanceRatios(
        understeerRatio = loadedSamples.count { it.handlingState == SessionAnalysisHandlingState.Understeer } / total,
        oversteerRatio = loadedSamples.count { it.handlingState == SessionAnalysisHandlingState.Oversteer } / total,
    )
}
