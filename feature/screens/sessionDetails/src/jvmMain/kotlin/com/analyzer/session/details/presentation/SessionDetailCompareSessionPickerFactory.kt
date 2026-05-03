package com.analyzer.session.details.presentation

import com.analyzer.session.details.domain.usecase.SessionDetailCompareCriteria
import com.analyzer.session.details.domain.usecase.SessionDetailCompareSuggestion
import com.analyzer.session.details.domain.usecase.SessionDetailCompareSuggestions
import com.analyzer.session.details.presentation.model.SessionDetailCompareSessionCandidateUi
import com.analyzer.session.details.presentation.model.SessionDetailCompareSessionPickerUi
import kotlinx.collections.immutable.toImmutableList

internal fun missingCompareSessionPickerUi(): SessionDetailCompareSessionPickerUi =
    SessionDetailCompareSessionPickerUi(
        isVisible = true,
        title = "Compare another session",
        supportingText = "Add a Sim Analyzer session folder, then compare only matching game and track layouts.",
        emptyMessage = "This session is missing the map identity needed to suggest a safe comparison.",
    )

internal fun SessionDetailCompareCriteria.toLoadingCompareSessionPickerUi(
    statusMessage: String? = null,
): SessionDetailCompareSessionPickerUi = SessionDetailCompareSessionPickerUi(
    isVisible = true,
    isLoading = true,
    title = compareSessionPickerTitle(),
    supportingText = compareSessionPickerSupportingText(),
    statusMessage = statusMessage,
)

internal fun SessionDetailCompareCriteria.toReadyCompareSessionPickerUi(
    suggestions: SessionDetailCompareSuggestions,
    highlightedSessionId: Long?,
    statusMessage: String? = null,
): SessionDetailCompareSessionPickerUi = SessionDetailCompareSessionPickerUi(
    isVisible = true,
    title = compareSessionPickerTitle(),
    supportingText = compareSessionPickerSupportingText(),
    statusMessage = statusMessage,
    emptyMessage = suggestions.toEmptyMessage(),
    candidates = suggestions.candidates
        .map { candidate -> candidate.toCompareSessionCandidateUi(highlightedSessionId) }
        .toImmutableList(),
)

private fun SessionDetailCompareCriteria.compareSessionPickerTitle(): String =
    "Compare on $trackLabel"

private fun SessionDetailCompareCriteria.compareSessionPickerSupportingText(): String =
    "Drop or choose a Sim Analyzer session folder. Only $gameLabel recordings from " +
        "the same track layout are listed below, and the same car is ranked first."

private fun SessionDetailCompareSuggestion.toCompareSessionCandidateUi(
    highlightedSessionId: Long?,
): SessionDetailCompareSessionCandidateUi = SessionDetailCompareSessionCandidateUi(
    sessionId = sessionId,
    carLabel = carLabel,
    sessionTypeLabel = sessionTypeLabel,
    dateLabel = dateLabel,
    timeLabel = timeLabel,
    bestLapLabel = bestLapLabel,
    lapsLabel = lapsLabel,
    recommendationLabel = if (sessionId == highlightedSessionId) {
        "Just added"
    } else {
        recommendationLabel
    },
)

private fun SessionDetailCompareSuggestions.toEmptyMessage(): String {
    if (candidates.isNotEmpty()) return ""
    if (excludedDifferentLayoutCount > 0) {
        return "Found sessions on the same track, but they use a different layout so they were excluded."
    }
    return "No comparable sessions from this track layout were found in your library yet."
}
