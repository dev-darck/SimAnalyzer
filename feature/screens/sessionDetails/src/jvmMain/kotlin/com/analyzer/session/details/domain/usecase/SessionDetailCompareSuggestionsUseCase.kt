package com.analyzer.session.details.domain.usecase

internal interface SessionDetailCompareSuggestionsUseCase {

    suspend fun loadSuggestions(
        criteria: SessionDetailCompareCriteria,
        forceRefresh: Boolean = false,
    ): SessionDetailCompareSuggestions
}
