package com.analyzer.session.details.domain.usecase

internal interface SessionDetailShareResultsUseCase {

    suspend fun copySummary(summaryText: String): SessionDetailShareResults

    suspend fun exportReport(
        directoryPath: String,
        reportFileName: String,
        summaryText: String,
    ): SessionDetailShareResults

    suspend fun openSessionFiles(sessionId: Long): SessionDetailShareResults
}
