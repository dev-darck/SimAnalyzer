package com.analyzer.session.analysis.domain.usecase

internal sealed interface SessionAnalysisShareResults {

    data object CopiedSummary : SessionAnalysisShareResults

    data class ExportedReport(val filePath: String) : SessionAnalysisShareResults

    data object OpenedSessionFiles : SessionAnalysisShareResults

    data object OpenedSessionFilesAndCopiedPath : SessionAnalysisShareResults

    data object CopiedSessionFilesPath : SessionAnalysisShareResults

    data class Failure(val reason: String) : SessionAnalysisShareResults
}
