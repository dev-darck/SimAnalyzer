package com.analyzer.session.presentation.model

import com.project.analyzer.ui.components.TrackMapData

data class SessionRowUi(
    val sessionId: Long,
    val dateLabel: String,
    val timeLabel: String,
    val gameId: String,
    val trackId: String,
    val gameLabel: String,
    val sessionTypeLabel: String,
    val trackLabel: String,
    val carLabel: String,
    val lapsLabel: String,
    val bestLapLabel: String,
    val isSaved: Boolean,
    val trackMap: TrackMapData? = null,
)
