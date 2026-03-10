package com.analyzer.session.data.model

public data class RecordedSessionDetailPage(
    val summary: RecordedSessionSummary,
    val stats: RecordedSessionDetailStats,
    val sessionTypeOptions: List<RecordedSessionOption> = emptyList(),
    val defaultSessionTypeId: String? = null,
    val selectedSessionTypeId: String? = null,
    val laps: List<LapSummary> = emptyList(),
    val page: Int = 1,
    val pageCount: Int = 1,
    val error: String? = null,
)

public data class RecordedSessionDetailStats(
    val bestLapTimeMs: Int?,
    val averageLapTimeMs: Int?,
    val incidentsCount: Int,
)
