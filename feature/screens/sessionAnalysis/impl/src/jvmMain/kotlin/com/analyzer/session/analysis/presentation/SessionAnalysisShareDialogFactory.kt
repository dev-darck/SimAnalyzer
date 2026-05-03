package com.analyzer.session.analysis.presentation

import com.analyzer.session.analysis.presentation.model.SessionAnalysisScreenMode
import com.analyzer.session.analysis.presentation.model.SessionAnalysisState
import com.analyzer.session.analysis.presentation.model.share.SessionAnalysisShareDialogUi
import com.project.analyzer.utils.toSlugId

internal fun SessionAnalysisState.toShareDialogUi(sessionId: Long): SessionAnalysisShareDialogUi {
    val header = header ?: return SessionAnalysisShareDialogUi()
    return SessionAnalysisShareDialogUi(
        isVisible = true,
        title = if (header.trackLabel.isNotBlank()) {
            "Share ${header.trackLabel} analysis"
        } else {
            "Share session analysis"
        },
        supportingText = "Choose a share-ready format: copy the chat summary, save a Markdown report, or open the source recording only when you need raw files.",
        summaryText = buildShareSummaryText(sessionId = sessionId),
        reportFileName = buildShareReportFileName(header = header, sessionId = sessionId),
    )
}

private fun SessionAnalysisState.buildShareSummaryText(sessionId: Long): String {
    val header = header ?: return ""
    val summary = summary
    return buildString {
        appendLine("Sim Analyzer Analysis Summary")
        appendLine()
        appendLine("Track: ${header.trackLabel.ifBlank { "Unknown track" }}")
        appendLine("Car: ${header.carLabel.ifBlank { "Unknown car" }}")
        appendLine("Session: ${header.sessionTypeLabel.ifBlank { "Unknown session" }}")
        appendLine("Started: ${header.startedAtLabel.ifBlank { "Session $sessionId" }}")
        appendLine("Mode: ${screenMode.toShareModeLabel()}")
        appendLine("Selected lap: ${header.selectedLapLabel.ifBlank { "--" }}")
        if (referenceLapNumber != null) {
            appendLine("Reference lap: $referenceLapNumber")
        }
        appendLine("Best lap: ${header.bestLapLabel.ifBlank { "--" }}")
        appendLine("Top speed: ${header.topSpeedLabel.ifBlank { "--" }}")
        summary?.let {
            appendLine()
            appendLine("Highlights")
            appendLine("- Lap delta: ${it.lapDeltaLabel}")
            appendLine("- Biggest loss: ${it.biggestLossLabel} (${it.biggestLossValueLabel})")
            appendLine("- Consistency: ${it.consistencyLabel}")
            appendLine("- Fuel: ${it.fuelLabel}")
            appendLine("- Top speed trend: ${it.topSpeedLabel}")
        }
        appendLine()
        append("Shared from Sim Analyzer")
    }
}

private fun buildShareReportFileName(
    header: com.analyzer.session.analysis.presentation.model.SessionAnalysisHeaderUi,
    sessionId: Long,
): String {
    val parts = listOf(
        "simanalyzer",
        header.trackLabel.ifBlank { "analysis" }.toSlugId(),
        "analysis",
        header.startedAtLabel.ifBlank { "session_$sessionId" }.toSlugId(),
    ).filter(String::isNotBlank)
    return parts.joinToString(separator = "_") + ".md"
}

private fun SessionAnalysisScreenMode.toShareModeLabel(): String = when (this) {
    SessionAnalysisScreenMode.Analysis -> "Analysis"
    SessionAnalysisScreenMode.Comparison -> "Comparison"
}
