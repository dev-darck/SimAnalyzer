package com.analyzer.session.domain.model

internal data class NormalizedTrackMapRequest(
    val requestedKey: String,
    val gameId: String,
    val trackId: String,
    val layoutId: String?,
    val sessionIds: List<Long>,
)
