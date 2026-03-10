package com.analyzer.session.domain.model

data class SessionListQuery(
    val gameId: String = "all",
    val trackId: String = "all",
    val carId: String = "all",
    val dateId: String = "all",
    val sortId: String = SESSION_LIST_SORT_NEWEST,
    val searchQuery: String = "",
    val page: Int = 1,
)
