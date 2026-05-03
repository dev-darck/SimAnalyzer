package com.analyzer.session.details.domain.usecase

internal interface SessionDetailImportCompareSessionUseCase {

    suspend fun importSession(
        criteria: SessionDetailCompareCriteria,
        path: String,
    ): SessionDetailImportCompareSessionResult
}
