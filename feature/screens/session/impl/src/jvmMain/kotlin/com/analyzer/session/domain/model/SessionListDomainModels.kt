package com.analyzer.session.domain.model

const val SESSION_LIST_SORT_BEST = "best"
const val SESSION_LIST_SORT_BEST_DESC = "best_desc"
const val SESSION_LIST_SORT_NEWEST = "newest"
const val SESSION_LIST_SORT_OLDEST = "oldest"
const val SESSION_LIST_SORT_GAME_ASC = "game_asc"
const val SESSION_LIST_SORT_GAME_DESC = "game_desc"
const val SESSION_LIST_SORT_TYPE_ASC = "type_asc"
const val SESSION_LIST_SORT_TYPE_DESC = "type_desc"
const val SESSION_LIST_SORT_TRACK_ASC = "track_asc"
const val SESSION_LIST_SORT_TRACK_DESC = "track_desc"
const val SESSION_LIST_SORT_CAR_ASC = "car_asc"
const val SESSION_LIST_SORT_CAR_DESC = "car_desc"
const val SESSION_LIST_SORT_LAPS_ASC = "laps_asc"
const val SESSION_LIST_SORT_LAPS_DESC = "laps_desc"

data class SessionListQuery(
    val gameId: String = "all",
    val trackId: String = "all",
    val carId: String = "all",
    val dateId: String = "all",
    val sortId: String = SESSION_LIST_SORT_NEWEST,
    val searchQuery: String = "",
    val page: Int = 1,
)

data class SessionFilterOption(val id: String, val label: String? = null)

data class SessionListDomainStats(
    val totalDistanceKm: Double = 0.0,
    val sessionsCount: Int = 0,
    val incidentsCount: Int = 0,
    val favoriteCar: String = "-",
)

data class SessionListDomainItem(
    val sessionId: Long,
    val startedAtMs: Long,
    val bestLapTimeMs: Int?,
    val lapCount: Int,
    val gameId: String,
    val gameLabel: String,
    val sessionTypeLabel: String,
    val trackId: String,
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

data class SessionListDataset(
    val items: List<SessionListDomainItem> = emptyList(),
    val stats: SessionListDomainStats = SessionListDomainStats(),
    val gameOptions: List<SessionFilterOption> = emptyList(),
    val trackOptions: List<SessionFilterOption> = emptyList(),
    val carOptions: List<SessionFilterOption> = emptyList(),
    val dateOptions: List<SessionFilterOption> = emptyList(),
    val sortOptions: List<SessionFilterOption> = emptyList(),
)

data class SessionListProjection(
    val rows: List<SessionListDomainItem>,
    val visibleRows: List<SessionListDomainItem>,
    val page: Int,
    val pageCount: Int,
    val error: String?,
)

data class SessionListResult(
    val dataset: SessionListDataset,
    val query: SessionListQuery,
    val projection: SessionListProjection,
)
