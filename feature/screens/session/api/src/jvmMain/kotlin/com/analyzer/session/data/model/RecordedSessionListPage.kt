package com.analyzer.session.data.model

public data class RecordedSessionListPage(
    val stats: RecordedSessionListStats = RecordedSessionListStats(),
    val gameOptions: List<RecordedSessionOption> = emptyList(),
    val trackOptions: List<RecordedSessionOption> = emptyList(),
    val carOptions: List<RecordedSessionOption> = emptyList(),
    val dateOptions: List<RecordedSessionOption> = emptyList(),
    val items: List<RecordedSessionSummary> = emptyList(),
    val page: Int = 1,
    val pageCount: Int = 1,
    val error: String? = null,
)

public data class RecordedSessionListStats(
    val totalDistanceKm: Double = 0.0,
    val sessionsCount: Int = 0,
    val incidentsCount: Int = 0,
    val favoriteCarModel: String? = null,
    val favoriteCarName: String? = null,
)
