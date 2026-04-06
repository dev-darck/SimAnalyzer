package com.analyzer.session.analysis.presentation.pipeline

import com.analyzer.session.analysis.presentation.formatter.formatDelta
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSummaryUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_summary_no_major_loss
import com.project.analyzer.telemetry.analysis.api.model.report.session.ComprehensiveSessionAnalysis
import org.jetbrains.compose.resources.getString
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Builds the session overview card from shell telemetry and upgrades the labels when the richer
 * analytics model is available.
 */
internal suspend fun buildSummary(
    laps: List<SessionAnalysisLapSummaryUi>,
    selectedLap: SessionAnalysisLapSummaryUi?,
    referenceLap: SessionAnalysisLapSummaryUi?,
    highlights: List<SessionAnalysisHighlightUi>,
    analytics: ComprehensiveSessionAnalysis? = null,
): SessionAnalysisSummaryUi {
    val lapDeltaMs = when {
        selectedLap?.durationMs != null && referenceLap?.durationMs != null ->
            selectedLap.durationMs - referenceLap.durationMs

        else -> selectedLap?.deltaToBestMs
    }

    val validDurations = laps.mapNotNull(SessionAnalysisLapSummaryUi::durationMs)
    val consistencyLabel = if (validDurations.size >= 2) {
        val mean = validDurations.average()
        val variance = validDurations.sumOf { duration -> (duration - mean) * (duration - mean) } / validDurations.size
        val spreadMs = sqrt(variance).roundToInt()
        "±$spreadMs ms"
    } else {
        "--"
    }

    val biggestLoss = highlights.maxByOrNull { it.deltaMs ?: Int.MIN_VALUE }
    val improvementArea = analytics?.sessionSummary?.top3ImprovementAreas?.firstOrNull()
    val consistencyOverride = analytics?.consistencyAnalysis?.consistencyScore
    val fuelOverride = analytics?.fuelAnalysis
    val speedOverride = analytics?.segmentAnalyses?.maxOfOrNull { segment -> segment.maxSpeed }

    return SessionAnalysisSummaryUi(
        lapDeltaLabel = formatDelta(lapDeltaMs),
        biggestLossLabel = improvementArea?.title ?: biggestLoss?.title ?: getString(
            Res.string.session_analysis_summary_no_major_loss,
        ),
        biggestLossValueLabel = improvementArea?.potentialGain?.toInt()?.let(
            ::formatDelta,
        ) ?: biggestLoss?.deltaMs?.let(::formatDelta) ?: "--",
        consistencyLabel = consistencyOverride?.let { value -> "$value/100" } ?: consistencyLabel,
        fuelLabel = fuelOverride?.let { fuel ->
            if (fuel.estimatedLapsRemaining > 0) {
                "${"%.2f".format(fuel.fuelPerLap)} L/lap"
            } else {
                "--"
            }
        } ?: selectedLap?.endFuelLiters?.let { value -> "${"%.1f".format(value)} L" }
            ?: selectedLap?.fuelUsedLiters?.let { value -> "-${"%.2f".format(value)} L/lap" }
            ?: "--",
        topSpeedLabel = speedOverride?.let { "${it.roundToInt()} km/h" }
            ?: selectedLap?.maxSpeedKmh?.let { "${it.roundToInt()} km/h" }
            ?: referenceLap?.maxSpeedKmh?.let { "${it.roundToInt()} km/h" }
            ?: "--",
    )
}
