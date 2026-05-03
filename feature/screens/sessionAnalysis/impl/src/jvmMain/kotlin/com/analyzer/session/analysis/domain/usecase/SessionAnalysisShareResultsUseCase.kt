package com.analyzer.session.analysis.domain.usecase

internal interface SessionAnalysisShareResultsUseCase {

    suspend fun copySummary(summaryText: String): SessionAnalysisShareResults

    suspend fun exportReport(
        directoryPath: String,
        reportFileName: String,
        summaryText: String,
    ): SessionAnalysisShareResults

    suspend fun openSessionFiles(sessionId: Long): SessionAnalysisShareResults
}
