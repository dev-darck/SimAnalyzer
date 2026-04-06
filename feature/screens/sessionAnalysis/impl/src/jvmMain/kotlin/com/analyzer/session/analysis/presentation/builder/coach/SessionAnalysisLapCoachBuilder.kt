@file:Suppress("LongParameterList")

package com.analyzer.session.analysis.presentation.builder.coach

import com.analyzer.session.analysis.presentation.formatter.formatLapTime
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapCoachUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_reference_lap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_coach_reference_lap_no_time
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.getString

/**
 * Builds lap coaching copy by combining lap deltas, diagnostic summaries, and curated highlights
 * into a compact explanation of where the selected lap diverged from the reference.
 */
internal suspend fun buildLapCoach(
    selectedLap: SessionAnalysisLap?,
    selectedSamples: List<SessionAnalysisSample>,
    referenceLap: SessionAnalysisLap?,
    referenceSamples: List<SessionAnalysisSample>,
    trackMap: SessionAnalysisTrackMap?,
    segmentSamples: List<SessionAnalysisSample> = selectedSamples,
    highlights: List<SessionAnalysisHighlight> = emptyList(),
    diagnosticSummary: SessionAnalysisDiagnosticSummaryUi? = null,
): SessionAnalysisLapCoachUi? {
    if (selectedLap == null || selectedSamples.size < 6) return null

    val strings = SessionAnalysisCoachStrings.resolve()
    val referenceProfile = ReferenceLapProfile(referenceSamples)
    val evaluation = evaluateLapCoach(
        SessionAnalysisCoachEvaluationInput(
            selectedSamples = selectedSamples,
            referenceSamples = referenceSamples,
            segmentSamples = segmentSamples,
            referenceProfile = referenceProfile,
            trackMap = trackMap,
            diagnosticSummary = diagnosticSummary,
        ),
        strings = strings,
    )
    val focus = buildCoachFocus(
        selectedSamples = selectedSamples,
        referenceProfile = referenceProfile,
        evaluation = evaluation,
        strings = strings,
    )
    val insights = (
        buildHighlightInsights(
            highlights = highlights,
            selectedLapNumber = selectedLap.lapNumber,
            strings = strings,
        ) + buildCoachInsights(
            selectedSamples = selectedSamples,
            referenceProfile = referenceProfile,
            evaluation = evaluation,
            strings = strings,
        )
        ).distinctBy { insight -> insight.title to insight.description }

    return SessionAnalysisLapCoachUi(
        referenceLapLabel = buildReferenceLapLabel(referenceLap, strings),
        summaryTitle = focus.title,
        summaryDescription = focus.description,
        metrics = buildCoachMetrics(
            evaluation = evaluation,
            strings = strings,
        ).sortedByDescending { metric -> metric.tone }.toImmutableList(),
        insights = insights.take(5).toImmutableList(),
    )
}

private suspend fun buildReferenceLapLabel(
    referenceLap: SessionAnalysisLap?,
    strings: SessionAnalysisCoachStrings,
): String = referenceLap?.let { lap ->
    lap.durationMs?.let { duration ->
        getString(Res.string.session_analysis_coach_reference_lap, lap.lapNumber, formatLapTime(duration))
    } ?: getString(Res.string.session_analysis_coach_reference_lap_no_time, lap.lapNumber)
} ?: strings.referenceLapUnavailable
