package com.analyzer.session.details.domain.usecase

internal data class SessionDetailCompareSuggestions(
    val candidates: List<SessionDetailCompareSuggestion>,
    val excludedDifferentLayoutCount: Int = 0,
)
