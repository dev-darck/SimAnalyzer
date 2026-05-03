package com.analyzer.session.details.presentation

import com.analyzer.session.details.domain.model.SessionDetailDomainHeader
import com.analyzer.session.details.domain.model.SessionDetailDomainStats
import com.analyzer.session.details.domain.model.SessionDetailPage
import com.analyzer.session.details.presentation.model.SessionDetailShareDialogUi
import com.project.analyzer.utils.toSlugId

internal fun SessionDetailPage.toShareDialogUi(sessionId: Long): SessionDetailShareDialogUi {
    val header = header
    return SessionDetailShareDialogUi(
        isVisible = true,
        title = if (header.trackLabel.isNotBlank()) {
            "Share ${header.trackLabel} session"
        } else {
            "Share session result"
        },
        supportingText = "Choose a share-ready format: copy the chat summary, save a Markdown report, or open the source recording only when you need raw files.",
        summaryText = buildShareSummaryText(header = header, stats = stats),
        reportFileName = buildShareReportFileName(header = header, sessionId = sessionId),
    )
}

private fun buildShareSummaryText(header: SessionDetailDomainHeader, stats: SessionDetailDomainStats): String =
    buildString {
        appendLine("Sim Analyzer Session Summary")
        appendLine()
        appendLine("Track: ${header.trackLabel.ifBlank { "Unknown track" }}")
        appendLine("Car: ${header.carLabel.ifBlank { "Unknown car" }}")
        appendLine("Session: ${header.sessionTypeLabel.ifBlank { "Unknown session" }}")
        if (header.subtitle.isNotBlank()) {
            appendLine("When: ${header.subtitle}")
        }
        appendLine("Air / Track: ${header.airTempLabel} / ${header.trackTempLabel}")
        appendLine()
        appendLine("Highlights")
        appendLine("- Best lap: ${stats.bestLapLabel}")
        appendLine("- Avg valid lap: ${stats.averageLapLabel}")
        appendLine("- Incidents: ${stats.incidentsCount}")
        appendLine()
        append("Shared from Sim Analyzer")
    }

private fun buildShareReportFileName(header: SessionDetailDomainHeader, sessionId: Long): String {
    val parts = listOf(
        "simanalyzer",
        header.trackLabel.ifBlank { "session" }.toSlugId(),
        header.sessionTypeLabel.ifBlank { "result" }.toSlugId(),
        header.subtitle.ifBlank { "session_$sessionId" }.toSlugId(),
    ).filter(String::isNotBlank)
    return parts.joinToString(separator = "_") + ".md"
}
