package com.analyzer.session.presentation.model

data class SessionListState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val stats: SessionStatsUi = SessionStatsUi(),
    val trackFilter: DropdownFilterUi = DropdownFilterUi(
        label = "Track",
        selectedId = FILTER_ALL_ID,
        selectedLabel = "All",
        options = listOf(DropdownOptionUi(FILTER_ALL_ID, "All")),
    ),
    val gameFilter: DropdownFilterUi = DropdownFilterUi(
        label = "Game",
        selectedId = FILTER_ALL_ID,
        selectedLabel = "All",
        options = listOf(DropdownOptionUi(FILTER_ALL_ID, "All")),
    ),
    val carFilter: DropdownFilterUi = DropdownFilterUi(
        label = "Car",
        selectedId = FILTER_ALL_ID,
        selectedLabel = "All",
        options = listOf(DropdownOptionUi(FILTER_ALL_ID, "All")),
    ),
    val dateFilter: DropdownFilterUi = DropdownFilterUi(
        label = "Date",
        selectedId = FILTER_ALL_ID,
        selectedLabel = "All",
        options = listOf(DropdownOptionUi(FILTER_ALL_ID, "All")),
    ),
    val sortFilter: DropdownFilterUi = DropdownFilterUi(
        label = "Sort by",
        selectedId = "best",
        selectedLabel = "Best lap",
        options = listOf(DropdownOptionUi("best", "Best lap")),
    ),
    val searchQuery: String = "",
    val page: Int = 1,
    val pageCount: Int = 1,
    val sessions: List<SessionRowUi> = emptyList(),
    val visibleSessions: List<SessionRowUi> = emptyList(),
)
