package com.analyzer.session.presentation.model

internal object SessionListSortIdsUi {
    const val Best = "best"
    const val BestDesc = "best_desc"
    const val Newest = "newest"
    const val Oldest = "oldest"
    const val GameAsc = "game_asc"
    const val GameDesc = "game_desc"
    const val TrackAsc = "track_asc"
    const val TrackDesc = "track_desc"
    const val CarAsc = "car_asc"
    const val CarDesc = "car_desc"
    const val LapsAsc = "laps_asc"
    const val LapsDesc = "laps_desc"
}

internal data class SessionListQueryUi(
    val gameId: String = FILTER_ALL_ID,
    val trackId: String = FILTER_ALL_ID,
    val carId: String = FILTER_ALL_ID,
    val dateId: String = FILTER_ALL_ID,
    val sortId: String = SessionListSortIdsUi.Newest,
    val searchQuery: String = "",
    val page: Int = 1,
)
