package com.analyzer.session.details.presentation

import com.analyzer.session.details.domain.usecase.SessionDetailImportCompareSessionResult
import com.analyzer.session.details.domain.usecase.SessionDetailShareResults
import com.analyzer.session.details.presentation.model.SessionDetailAction
import com.project.analyzer.ui.components.InfoBarSeverity
import java.io.File

internal data class SessionDetailCompareImportFeedback(
    val action: SessionDetailAction,
    val shouldReloadSuggestions: Boolean,
    val highlightedSessionId: Long? = null,
    val statusMessage: String? = null,
)

internal fun SessionDetailImportCompareSessionResult.toCompareImportFeedback(): SessionDetailCompareImportFeedback =
    when (this) {
        is SessionDetailImportCompareSessionResult.Imported -> SessionDetailCompareImportFeedback(
            action = SessionDetailAction(
                title = "Compare session added",
                message = "It is ready below. Press Compare on the session you want.",
                severity = InfoBarSeverity.Success,
            ),
            shouldReloadSuggestions = true,
            highlightedSessionId = sessionId,
            statusMessage = "Session added. Pick it below to open the comparison.",
        )

        is SessionDetailImportCompareSessionResult.AlreadyAvailable -> SessionDetailCompareImportFeedback(
            action = SessionDetailAction(
                title = "Session already available",
                message = "Pick it below to start the comparison.",
                severity = InfoBarSeverity.Info,
            ),
            shouldReloadSuggestions = true,
            highlightedSessionId = sessionId,
            statusMessage = "This recording is already in your library. Pick it below to compare.",
        )

        is SessionDetailImportCompareSessionResult.Rejected -> SessionDetailCompareImportFeedback(
            action = SessionDetailAction(
                title = "Session not added",
                message = reason,
                severity = InfoBarSeverity.Warning,
            ),
            shouldReloadSuggestions = false,
        )

        is SessionDetailImportCompareSessionResult.Failure -> SessionDetailCompareImportFeedback(
            action = SessionDetailAction(
                title = "Import failed",
                message = reason,
                severity = InfoBarSeverity.Error,
            ),
            shouldReloadSuggestions = false,
        )
    }

internal fun SessionDetailShareResults.toCopySummaryAction(): SessionDetailAction? = when (this) {
    SessionDetailShareResults.CopiedSummary -> SessionDetailAction(
        title = "Summary copied",
        message = "Paste it anywhere and send it.",
        severity = InfoBarSeverity.Success,
    )

    is SessionDetailShareResults.Failure -> SessionDetailAction(
        title = "Copy failed",
        message = reason,
        severity = InfoBarSeverity.Error,
    )

    else -> null
}

internal fun SessionDetailShareResults.toExportReportAction(): SessionDetailAction? = when (this) {
    is SessionDetailShareResults.ExportedReport -> SessionDetailAction(
        title = "Report exported",
        message = "Saved ${File(filePath).name} to the selected folder.",
        severity = InfoBarSeverity.Success,
    )

    is SessionDetailShareResults.Failure -> SessionDetailAction(
        title = "Export failed",
        message = reason,
        severity = InfoBarSeverity.Error,
    )

    else -> null
}

internal fun SessionDetailShareResults.toOpenSessionFilesAction(): SessionDetailAction? = when (this) {
    SessionDetailShareResults.OpenedSessionFilesAndCopiedPath -> SessionDetailAction(
        title = "Raw files ready",
        message = "Opened the recording folder and copied its path.",
        severity = InfoBarSeverity.Success,
    )

    SessionDetailShareResults.OpenedSessionFiles -> SessionDetailAction(
        title = "Raw files opened",
        message = "The recording folder is open in Explorer.",
        severity = InfoBarSeverity.Success,
    )

    SessionDetailShareResults.CopiedSessionFilesPath -> SessionDetailAction(
        title = "Raw file path copied",
        message = "The recording folder could not be opened, but its path is in the clipboard.",
        severity = InfoBarSeverity.Info,
    )

    is SessionDetailShareResults.Failure -> SessionDetailAction(
        title = "Open raw files failed",
        message = reason,
        severity = InfoBarSeverity.Error,
    )

    else -> null
}
