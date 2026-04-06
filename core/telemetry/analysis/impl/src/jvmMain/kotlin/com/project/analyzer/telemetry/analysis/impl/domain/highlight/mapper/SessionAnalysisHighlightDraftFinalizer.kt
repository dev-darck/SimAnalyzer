package com.project.analyzer.telemetry.analysis.impl.domain.highlight.mapper

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.SessionAnalysisHighlightDraft

internal fun List<SessionAnalysisHighlightDraft>.finalizeHighlights(): List<SessionAnalysisHighlight> {
    val distinctDrafts = filter { draft -> draft.title.isNotBlank() && draft.description.isNotBlank() }
        .distinctBy { draft ->
            listOf(
                draft.category.name,
                draft.segmentId.toString(),
                draft.lapNumber.toString(),
                draft.cornerNumber?.toString().orEmpty(),
                draft.trackPosition?.let { position -> "%.3f".format(position) }.orEmpty(),
                draft.title,
            ).joinToString("|")
        }
        .sortedWith(
            compareByDescending<SessionAnalysisHighlightDraft> { draft ->
                computeHighlightPriority(
                    draft.category,
                    draft.deltaMs,
                )
            }
                .thenByDescending { draft -> draft.severity.rank() }
                .thenByDescending { draft -> draft.deltaMs ?: 0 }
                .thenBy(SessionAnalysisHighlightDraft::segmentId)
                .thenBy(SessionAnalysisHighlightDraft::lapNumber),
        )
    if (distinctDrafts.isEmpty()) return emptyList()

    val idsByDraft = distinctDrafts.associateWith { draft -> draft.toHighlightId() }
    return distinctDrafts.map { draft ->
        SessionAnalysisHighlight(
            id = idsByDraft.getValue(draft),
            category = draft.category,
            severity = draft.severity,
            segmentId = draft.segmentId,
            lapNumber = draft.lapNumber,
            sampleIndexInLap = draft.sampleIndexInLap,
            trackPosition = draft.trackPosition,
            title = draft.title,
            description = draft.description,
            deltaMs = draft.deltaMs,
            diagnosisSource = draft.diagnosisSource,
            recommendation = draft.recommendation,
            cornerNumber = draft.cornerNumber,
            score = draft.score,
            priority = computeHighlightPriority(draft.category, draft.deltaMs),
            affectedLaps = draft.affectedLaps,
            relatedHighlightIds = distinctDrafts
                .filter { other -> other != draft && draft.isRelatedTo(other) }
                .map(idsByDraft::getValue)
                .take(4),
        )
    }
}

private fun computeHighlightPriority(category: SessionAnalysisHighlightCategory, deltaMs: Int?): Int = when {
    category == SessionAnalysisHighlightCategory.TimeLoss && (deltaMs ?: 0) > 200 -> 10
    category == SessionAnalysisHighlightCategory.SetupUndersteer -> 9
    category == SessionAnalysisHighlightCategory.SetupOversteer -> 9
    category == SessionAnalysisHighlightCategory.TyrePressureImbalance -> 8
    category == SessionAnalysisHighlightCategory.TyreTempImbalance -> 8
    category == SessionAnalysisHighlightCategory.AeroBalance -> 8
    category == SessionAnalysisHighlightCategory.BrakeBalance -> 8
    category == SessionAnalysisHighlightCategory.CoastingZone -> 7
    category == SessionAnalysisHighlightCategory.Understeer -> 7
    category == SessionAnalysisHighlightCategory.Oversteer -> 7
    category == SessionAnalysisHighlightCategory.TrailBrakingMissing -> 6
    category == SessionAnalysisHighlightCategory.EarlyApexEntry -> 6
    category == SessionAnalysisHighlightCategory.LateApexEntry -> 6
    category == SessionAnalysisHighlightCategory.WheelLockup -> 5
    category == SessionAnalysisHighlightCategory.WheelSpin -> 5
    category == SessionAnalysisHighlightCategory.InconsistentLine -> 4
    category == SessionAnalysisHighlightCategory.DamperIssue -> 4
    category == SessionAnalysisHighlightCategory.TopSpeed -> 2
    category == SessionAnalysisHighlightCategory.ThrottleCommitment -> 2
    else -> 3
}

private fun SessionAnalysisHighlightDraft.isRelatedTo(other: SessionAnalysisHighlightDraft): Boolean {
    if (segmentId != other.segmentId || category == other.category) return false
    if (cornerNumber != null && other.cornerNumber != null && cornerNumber == other.cornerNumber) {
        return true
    }
    val balanceCategories = setOf(
        SessionAnalysisHighlightCategory.Understeer,
        SessionAnalysisHighlightCategory.Oversteer,
    )
    val setupCategories = setOf(
        SessionAnalysisHighlightCategory.SetupUndersteer,
        SessionAnalysisHighlightCategory.SetupOversteer,
        SessionAnalysisHighlightCategory.TyreTempImbalance,
        SessionAnalysisHighlightCategory.TyrePressureImbalance,
        SessionAnalysisHighlightCategory.AeroBalance,
        SessionAnalysisHighlightCategory.BrakeBalance,
    )
    return (category in balanceCategories && other.category in setupCategories) ||
        (other.category in balanceCategories && category in setupCategories)
}

private fun SessionAnalysisHighlightDraft.toHighlightId(): String = buildString {
    append(category.name.lowercase())
    append('-')
    append(segmentId)
    append('-')
    append(lapNumber)
    append('-')
    append(cornerNumber ?: "na")
    append('-')
    append(sampleIndexInLap)
}

private fun SessionAnalysisHighlightSeverity.rank(): Int = when (this) {
    SessionAnalysisHighlightSeverity.Positive -> 1
    SessionAnalysisHighlightSeverity.Warning -> 2
    SessionAnalysisHighlightSeverity.Critical -> 3
}
