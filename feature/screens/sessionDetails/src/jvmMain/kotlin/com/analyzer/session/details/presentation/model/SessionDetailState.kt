package com.analyzer.session.details.presentation.model

data class SessionDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val header: SessionDetailHeaderUi = SessionDetailHeaderUi(),
    val stats: SessionDetailStatsUi = SessionDetailStatsUi(),
    val sortFilter: DropdownFilterUi = DropdownFilterUi(
        label = "Sort by",
        selectedId = "lap",
        selectedLabel = "Lap",
        options = listOf(DropdownOptionUi("lap", "Lap"))
    ),
    val showFilter: DropdownFilterUi = DropdownFilterUi(
        label = "Show",
        selectedId = "all",
        selectedLabel = "All laps",
        options = listOf(DropdownOptionUi("all", "All laps"))
    ),
    val page: Int = 1,
    val pageCount: Int = 1,
    val laps: List<SessionLapRowUi> = emptyList(),
    val visibleLaps: List<SessionLapRowUi> = emptyList(),
)
