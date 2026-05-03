package com.analyzer.session.details.domain.usecase

internal data class SessionDetailCompareCriteria(
    val currentSessionId: Long,
    val gameId: String,
    val gameLabel: String,
    val trackId: String,
    val layoutId: String? = null,
    val trackLabel: String,
    val preferredCarIdentityKey: String? = null,
)
