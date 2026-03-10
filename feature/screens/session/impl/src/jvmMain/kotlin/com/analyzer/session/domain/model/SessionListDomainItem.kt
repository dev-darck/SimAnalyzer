package com.analyzer.session.domain.model

data class SessionListDomainItem(
    val sessionId: Long,
    val startedAtMs: Long,
    val bestLapTimeMs: Int?,
    val lapCount: Int,
    val gameId: String,
    val gameLabel: String,
    val sessionTypeLabel: String,
    val trackId: String,
    val layoutId: String? = null,
    val trackLabel: String,
    val carId: String,
    val carLabel: String,
    val dateLabel: String,
    val timeLabel: String,
    val lapsLabel: String,
    val bestLapLabel: String,
    val isSaved: Boolean,
    val searchText: String,
)
