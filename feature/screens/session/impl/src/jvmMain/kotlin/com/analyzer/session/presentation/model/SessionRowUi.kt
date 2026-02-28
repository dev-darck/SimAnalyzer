package com.analyzer.session.presentation.model

data class SessionRowUi(
    val sessionId: Long,
    val dateLabel: String,
    val timeLabel: String,
    val gameLabel: String,
    val sessionTypeLabel: String,
    val trackLabel: String,
    val carLabel: String,
    val lapsLabel: String,
    val bestLapLabel: String,
    val isSaved: Boolean,
)
