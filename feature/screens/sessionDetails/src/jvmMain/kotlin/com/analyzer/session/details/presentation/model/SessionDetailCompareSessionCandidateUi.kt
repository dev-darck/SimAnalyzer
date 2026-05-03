package com.analyzer.session.details.presentation.model

data class SessionDetailCompareSessionCandidateUi(
    val sessionId: Long,
    val carLabel: String,
    val sessionTypeLabel: String,
    val dateLabel: String,
    val timeLabel: String,
    val bestLapLabel: String,
    val lapsLabel: String,
    val recommendationLabel: String? = null,
)
