package com.analyzer.session.data.model

public data class RecordedSessionSummary(
    val sessionId: Long,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val gameId: String,
    val sessionType: String?,
    val carModel: String?,
    val trackId: String?,
    val lapCount: Int,
    val bestLapTimeMs: Int?,
    val totalIncidents: Int,
    val distanceKm: Double,
    val isSaved: Boolean,
    val airTempC: Float? = null,
    val trackTempC: Float? = null,
)
