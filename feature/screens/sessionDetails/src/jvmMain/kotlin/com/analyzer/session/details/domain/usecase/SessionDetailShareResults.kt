package com.analyzer.session.details.domain.usecase

internal sealed interface SessionDetailShareResults {
    data object CopiedSummary : SessionDetailShareResults
    data class ExportedReport(val filePath: String) : SessionDetailShareResults
    data object OpenedSessionFilesAndCopiedPath : SessionDetailShareResults
    data object OpenedSessionFiles : SessionDetailShareResults
    data object CopiedSessionFilesPath : SessionDetailShareResults
    data class Failure(val reason: String) : SessionDetailShareResults
}
