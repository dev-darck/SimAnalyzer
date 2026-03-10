package com.analyzer.session.presentation.model

import com.analyzer.session.domain.model.SESSION_LIST_SORT_NEWEST

data class SessionListState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val stats: SessionStatsUi = SessionStatsUi(),
    val trackFilter: SessionFilterUiModel = SessionFilterUiModel(
        kind = SessionFilterKind.Track,
        selectedId = FILTER_ALL_ID,
        options = listOf(SessionFilterOptionUi(FILTER_ALL_ID)),
    ),
    val gameFilter: SessionFilterUiModel = SessionFilterUiModel(
        kind = SessionFilterKind.Game,
        selectedId = FILTER_ALL_ID,
        options = listOf(SessionFilterOptionUi(FILTER_ALL_ID)),
    ),
    val carFilter: SessionFilterUiModel = SessionFilterUiModel(
        kind = SessionFilterKind.Car,
        selectedId = FILTER_ALL_ID,
        options = listOf(SessionFilterOptionUi(FILTER_ALL_ID)),
    ),
    val dateFilter: SessionFilterUiModel = SessionFilterUiModel(
        kind = SessionFilterKind.Date,
        selectedId = FILTER_ALL_ID,
        options = listOf(SessionFilterOptionUi(FILTER_ALL_ID)),
    ),
    val sortFilter: SessionFilterUiModel = SessionFilterUiModel(
        kind = SessionFilterKind.Sort,
        selectedId = SESSION_LIST_SORT_NEWEST,
        options = listOf(SessionFilterOptionUi(SESSION_LIST_SORT_NEWEST)),
    ),
    val searchQuery: String = "",
    val page: Int = 1,
    val pageCount: Int = 1,
    val visibleSessions: List<SessionRowUi> = emptyList(),
)
