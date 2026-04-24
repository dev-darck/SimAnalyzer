package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.builder.diagnostic.buildDiagnosticSummary
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosisSourceUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightSeverityUi
import kotlin.math.abs

/**
 * Resolves highlight state alongside diagnostics so the workspace can surface the strongest cues immediately.
 */
internal fun mergeHighlights(
    reportHighlights: List<SessionAnalysisHighlightUi>,
    generatedHighlights: List<SessionAnalysisHighlightUi>,
): List<SessionAnalysisHighlightUi> = (reportHighlights + generatedHighlights)
    .distinctBy(SessionAnalysisHighlightUi::stableHighlightKey)
    .sortedWith(
        compareByDescending<SessionAnalysisHighlightUi>(SessionAnalysisHighlightUi::priority)
            .thenByDescending { highlight -> highlight.deltaMs ?: 0 }
            .thenByDescending { highlight -> highlight.severity.rank() }
            .thenBy(SessionAnalysisHighlightUi::lapNumber),
    )

internal fun resolveVisibleHighlights(
    highlights: List<SessionAnalysisHighlightUi>,
    selectedLapNumber: Int?,
): List<SessionAnalysisHighlightUi> {
    if (highlights.isEmpty()) return emptyList()
    val lapScopedHighlights = highlights.filter { highlight ->
        highlight.isRelevantToLap(selectedLapNumber)
    }
    return if (lapScopedHighlights.isNotEmpty()) {
        lapScopedHighlights
    } else {
        highlights
    }
}

internal fun resolveDiagnosticSummary(
    visibleHighlights: List<SessionAnalysisHighlightUi>,
    fallbackHighlights: List<SessionAnalysisHighlightUi>,
): SessionAnalysisDiagnosticSummaryUi? = buildDiagnosticSummary(visibleHighlights)
    ?: buildDiagnosticSummary(fallbackHighlights)

internal fun Iterable<SessionAnalysisComparisonPointUi>.nearestToTrackPosition(
    trackPosition: Float,
): SessionAnalysisComparisonPointUi? = minByOrNull { point -> abs(point.trackPosition - trackPosition) }

private fun SessionAnalysisHighlightUi.isRelevantToLap(selectedLapNumber: Int?): Boolean {
    if (selectedLapNumber == null) return true
    if (diagnosisSource != SessionAnalysisDiagnosisSourceUi.DrivingStyle) return true
    return lapNumber == selectedLapNumber || selectedLapNumber in affectedLaps
}

private fun SessionAnalysisHighlightUi.stableHighlightKey(): String = buildString {
    append(id.ifBlank { "${category.name}-$lapNumber-${cornerNumber ?: -1}-$title" })
    append('|')
    append(trackPosition?.let { trackPosition -> (trackPosition * 1000f).toInt() } ?: -1)
}

private fun SessionAnalysisHighlightSeverityUi.rank(): Int = when (this) {
    SessionAnalysisHighlightSeverityUi.Positive -> 1
    SessionAnalysisHighlightSeverityUi.Warning -> 2
    SessionAnalysisHighlightSeverityUi.Critical -> 3
}
