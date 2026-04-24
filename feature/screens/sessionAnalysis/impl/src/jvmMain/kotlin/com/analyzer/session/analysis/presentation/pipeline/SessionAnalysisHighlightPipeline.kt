package com.analyzer.session.analysis.presentation.pipeline

import com.analyzer.session.analysis.presentation.formatter.formatDelta
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightCategoryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightSeverityUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_biggest_loss_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_biggest_loss_recommendation
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_biggest_loss_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_brake_mismatch_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_brake_mismatch_recommendation
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_brake_mismatch_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_entry_overslowing_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_entry_overslowing_recommendation
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_entry_overslowing_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_late_throttle_description
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_late_throttle_recommendation
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_generated_highlight_late_throttle_title
import org.jetbrains.compose.resources.getString
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Reorders and condenses highlight cues so the workspace surfaces the most actionable items first.
 */
internal suspend fun buildHighlights(
    comparisonPoints: List<SessionAnalysisComparisonPointUi>,
    selectedLapNumber: Int?,
): List<SessionAnalysisHighlightUi> {
    if (comparisonPoints.size < 4) return emptyList()

    val lapNumber = selectedLapNumber ?: 0
    val biggestLoss = comparisonPoints.maxByOrNull { point -> point.deltaMs ?: Int.MIN_VALUE }
    val speedDeficit = comparisonPoints.maxByOrNull { point ->
        ((point.referenceSpeedKmh ?: 0f) - (point.selectedSpeedKmh ?: 0f)).roundToInt()
    }
    val throttleDelay = comparisonPoints.maxByOrNull { point ->
        val delta = (point.referenceThrottle ?: 0f) - (point.selectedThrottle ?: 0f)
        if (delta > 0f) (delta * 1000f).roundToInt() else Int.MIN_VALUE
    }
    val brakeDelay = comparisonPoints.maxByOrNull { point ->
        val delta = (point.selectedBrake ?: 0f) - (point.referenceBrake ?: 0f)
        abs(delta * 1000f).roundToInt()
    }

    return buildList {
        biggestLoss?.deltaMs?.takeIf { it > 80 }?.let { delta ->
            add(
                SessionAnalysisHighlightUi(
                    category = SessionAnalysisHighlightCategoryUi.TimeLoss,
                    severity = SessionAnalysisHighlightSeverityUi.Critical,
                    lapNumber = lapNumber,
                    title = getString(Res.string.session_analysis_generated_highlight_biggest_loss_title),
                    description = getString(
                        Res.string.session_analysis_generated_highlight_biggest_loss_description,
                        formatDelta(delta),
                        formatTrackPosition(point = biggestLoss),
                    ),
                    trackPosition = biggestLoss.trackPosition,
                    deltaMs = delta,
                    recommendation = getString(
                        Res.string.session_analysis_generated_highlight_biggest_loss_recommendation,
                    ),
                ),
            )
        }
        speedDeficit?.let { point ->
            val delta = ((point.referenceSpeedKmh ?: return@let) - (point.selectedSpeedKmh ?: return@let)).roundToInt()
            if (delta >= 5) {
                add(
                    SessionAnalysisHighlightUi(
                        category = SessionAnalysisHighlightCategoryUi.BrakePoint,
                        severity = SessionAnalysisHighlightSeverityUi.Warning,
                        lapNumber = lapNumber,
                        title = getString(Res.string.session_analysis_generated_highlight_entry_overslowing_title),
                        description = getString(
                            Res.string.session_analysis_generated_highlight_entry_overslowing_description,
                            delta,
                            formatTrackPosition(point),
                        ),
                        trackPosition = point.trackPosition,
                        deltaMs = point.deltaMs,
                        recommendation = getString(
                            Res.string.session_analysis_generated_highlight_entry_overslowing_recommendation,
                        ),
                    ),
                )
            }
        }
        throttleDelay?.let { point ->
            val delta = ((point.referenceThrottle ?: return@let) - (point.selectedThrottle ?: return@let)) * 100f
            if (delta >= 18f) {
                add(
                    SessionAnalysisHighlightUi(
                        category = SessionAnalysisHighlightCategoryUi.ThrottleCommitment,
                        severity = SessionAnalysisHighlightSeverityUi.Warning,
                        lapNumber = lapNumber,
                        title = getString(Res.string.session_analysis_generated_highlight_late_throttle_title),
                        description = getString(
                            Res.string.session_analysis_generated_highlight_late_throttle_description,
                            delta.roundToInt(),
                            formatTrackPosition(point),
                        ),
                        trackPosition = point.trackPosition,
                        deltaMs = point.deltaMs,
                        recommendation = getString(
                            Res.string.session_analysis_generated_highlight_late_throttle_recommendation,
                        ),
                    ),
                )
            }
        }
        brakeDelay?.let { point ->
            val selectedBrake = point.selectedBrake ?: return@let
            val referenceBrake = point.referenceBrake ?: return@let
            if (abs(selectedBrake - referenceBrake) >= 0.22f) {
                add(
                    SessionAnalysisHighlightUi(
                        category = SessionAnalysisHighlightCategoryUi.BrakePoint,
                        severity = SessionAnalysisHighlightSeverityUi.Warning,
                        lapNumber = lapNumber,
                        title = getString(Res.string.session_analysis_generated_highlight_brake_mismatch_title),
                        description = getString(
                            Res.string.session_analysis_generated_highlight_brake_mismatch_description,
                            formatTrackPosition(point),
                        ),
                        trackPosition = point.trackPosition,
                        deltaMs = point.deltaMs,
                        recommendation = getString(
                            Res.string.session_analysis_generated_highlight_brake_mismatch_recommendation,
                        ),
                    ),
                )
            }
        }
    }.sortedByDescending { highlight ->
        when (highlight.severity) {
            SessionAnalysisHighlightSeverityUi.Critical -> 3
            SessionAnalysisHighlightSeverityUi.Warning -> 2
            SessionAnalysisHighlightSeverityUi.Positive -> 1
        }
    }
}

private fun formatTrackPosition(point: SessionAnalysisComparisonPointUi): String =
    "${(point.trackPosition * 100f).roundToInt()}%"
