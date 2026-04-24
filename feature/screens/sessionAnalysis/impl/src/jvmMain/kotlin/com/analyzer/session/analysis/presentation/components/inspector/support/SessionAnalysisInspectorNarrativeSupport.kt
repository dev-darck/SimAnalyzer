package com.analyzer.session.analysis.presentation.components.inspector.support

import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightCategoryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapCoachUi

internal fun resolveLineNarratives(
    diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?,
    lapCoach: SessionAnalysisLapCoachUi?,
    highlights: List<SessionAnalysisHighlightUi>,
): List<SessionAnalysisInspectorNarrativeItem> {
    val summaryNarratives = diagnosticSummary
        ?.topDrivingIssues
        .orEmpty()
        .asSequence()
        .filter(DiagnosticIssueUi::isLineRelevant)
        .take(3)
        .map(DiagnosticIssueUi::toLineNarrative)
        .toList()
    if (summaryNarratives.isNotEmpty()) return summaryNarratives

    val rawLineHighlights = highlights
        .asSequence()
        .filter(SessionAnalysisHighlightUi::isLineRelevant)
        .toList()
    val lineHighlights = rawLineHighlights
        .asSequence()
        .filterNot { highlight -> highlight.isFallbackTimeLossForSameCorner(rawLineHighlights) }
        .toList()
    val highlightNarratives = lineHighlights
        .asSequence()
        .sortedWith(
            compareByDescending<SessionAnalysisHighlightUi> { highlight -> highlight.priority }
                .thenByDescending { highlight -> highlight.deltaMs ?: 0 }
                .thenByDescending { highlight -> highlight.affectedLaps.size },
        )
        .distinctBy(SessionAnalysisHighlightUi::lineNarrativeGroupKey)
        .map(SessionAnalysisHighlightUi::toLineNarrative)
        .take(3)
        .toList()
    if (highlightNarratives.isNotEmpty()) return highlightNarratives

    return lapCoach
        ?.insights
        ?.asSequence()
        ?.sortedByDescending { insight -> insight.tone }
        ?.filterNot { insight -> insight.description.isBlank() }
        ?.distinctBy { insight -> insight.title.lowercase() }
        ?.take(3)
        ?.map { insight ->
            SessionAnalysisInspectorNarrativeItem(
                title = insight.title,
                description = insight.description,
                lookAt = "Reference trace and input timing",
            )
        }
        ?.toList()
        .orEmpty()
}

private fun DiagnosticIssueUi.isLineRelevant(): Boolean = when (category) {
    SessionAnalysisHighlightCategoryUi.BrakePoint,
    SessionAnalysisHighlightCategoryUi.TrailBrakingMissing,
    SessionAnalysisHighlightCategoryUi.EarlyApexEntry,
    SessionAnalysisHighlightCategoryUi.LateApexEntry,
    SessionAnalysisHighlightCategoryUi.CoastingZone,
    SessionAnalysisHighlightCategoryUi.InconsistentLine,
    SessionAnalysisHighlightCategoryUi.WheelLockup,
    SessionAnalysisHighlightCategoryUi.WheelSpin,
    SessionAnalysisHighlightCategoryUi.ThrottleCommitment,
    SessionAnalysisHighlightCategoryUi.TimeLoss,
    SessionAnalysisHighlightCategoryUi.Understeer,
    SessionAnalysisHighlightCategoryUi.Oversteer,
    -> true

    else -> false
}

private fun SessionAnalysisHighlightUi.isLineRelevant(): Boolean = when (category) {
    SessionAnalysisHighlightCategoryUi.BrakePoint,
    SessionAnalysisHighlightCategoryUi.TrailBrakingMissing,
    SessionAnalysisHighlightCategoryUi.EarlyApexEntry,
    SessionAnalysisHighlightCategoryUi.LateApexEntry,
    SessionAnalysisHighlightCategoryUi.CoastingZone,
    SessionAnalysisHighlightCategoryUi.InconsistentLine,
    SessionAnalysisHighlightCategoryUi.WheelLockup,
    SessionAnalysisHighlightCategoryUi.WheelSpin,
    SessionAnalysisHighlightCategoryUi.ThrottleCommitment,
    SessionAnalysisHighlightCategoryUi.TimeLoss,
    SessionAnalysisHighlightCategoryUi.Understeer,
    SessionAnalysisHighlightCategoryUi.Oversteer,
    -> true

    else -> false
}

private fun SessionAnalysisHighlightUi.isFallbackTimeLossForSameCorner(
    lineHighlights: List<SessionAnalysisHighlightUi>,
): Boolean {
    if (category != SessionAnalysisHighlightCategoryUi.TimeLoss || cornerNumber == null) {
        return false
    }
    return lineHighlights.any { other ->
        other !== this &&
            other.cornerNumber == cornerNumber &&
            other.category != SessionAnalysisHighlightCategoryUi.TimeLoss
    }
}

private fun SessionAnalysisHighlightUi.lineNarrativeGroupKey(): String = when (category) {
    SessionAnalysisHighlightCategoryUi.BrakePoint,
    SessionAnalysisHighlightCategoryUi.TrailBrakingMissing,
    SessionAnalysisHighlightCategoryUi.WheelLockup,
    -> "entry"

    SessionAnalysisHighlightCategoryUi.EarlyApexEntry,
    SessionAnalysisHighlightCategoryUi.LateApexEntry,
    SessionAnalysisHighlightCategoryUi.InconsistentLine,
    -> "apex"

    SessionAnalysisHighlightCategoryUi.CoastingZone,
    SessionAnalysisHighlightCategoryUi.WheelSpin,
    SessionAnalysisHighlightCategoryUi.ThrottleCommitment,
    -> "exit"

    SessionAnalysisHighlightCategoryUi.Understeer,
    SessionAnalysisHighlightCategoryUi.Oversteer,
    -> "balance"

    else -> "loss"
}

private fun SessionAnalysisHighlightUi.toLineNarrative(): SessionAnalysisInspectorNarrativeItem =
    SessionAnalysisInspectorNarrativeItem(
        title = lineNarrativeTitle(),
        description = buildString {
            cornerNumber?.let { corner ->
                append("In turn ")
                append(corner)
                append(", ")
            }
            append(description.ifBlank { title }.lowercaseFirst().asSentence())
            deltaMs?.takeIf { delta -> delta > 0 }?.let { delta ->
                append(" Current loss here is ")
                append(delta)
                append(" ms to the reference.")
            }
        }.trim(),
        recommendation = recommendation.trim(),
        lookAt = category.toDrivingLookAtLabel(cornerNumber),
        source = diagnosisSource,
    )

private fun DiagnosticIssueUi.toLineNarrative(): SessionAnalysisInspectorNarrativeItem =
    SessionAnalysisInspectorNarrativeItem(
        title = title,
        description = buildString {
            append(description.lowercaseFirst().asSentence())
            if (potentialTimeGainMs > 0) {
                append(" Current loss here is ")
                append(potentialTimeGainMs)
                append(" ms to the reference.")
            }
        }.trim(),
        recommendation = recommendation.trim(),
        lookAt = lineLookAtLabel(),
        source = source,
    )

private fun SessionAnalysisHighlightUi.lineNarrativeTitle(): String = when (category) {
    SessionAnalysisHighlightCategoryUi.BrakePoint,
    SessionAnalysisHighlightCategoryUi.TrailBrakingMissing,
    SessionAnalysisHighlightCategoryUi.WheelLockup,
    -> cornerScopedTitle("Entry braking is costing time")

    SessionAnalysisHighlightCategoryUi.EarlyApexEntry,
    SessionAnalysisHighlightCategoryUi.LateApexEntry,
    SessionAnalysisHighlightCategoryUi.InconsistentLine,
    -> cornerScopedTitle("Mid-corner line is costing time")

    SessionAnalysisHighlightCategoryUi.CoastingZone,
    SessionAnalysisHighlightCategoryUi.WheelSpin,
    SessionAnalysisHighlightCategoryUi.ThrottleCommitment,
    -> cornerScopedTitle("Exit throttle timing is costing time")

    SessionAnalysisHighlightCategoryUi.Understeer,
    SessionAnalysisHighlightCategoryUi.Oversteer,
    -> cornerScopedTitle("Balance is costing time")

    else -> cornerScopedTitle("Primary loss zone")
}

private fun String.asSentence(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return trimmed
    return if (trimmed.endsWith(".")) trimmed else "$trimmed."
}

private fun SessionAnalysisHighlightUi.cornerScopedTitle(text: String): String =
    cornerNumber?.let { corner -> "Turn $corner: $text" } ?: text

private fun String.lowercaseFirst(): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return trimmed
    val first = trimmed.first()
    return first.lowercaseChar() + trimmed.drop(1)
}
