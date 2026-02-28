package com.analyzer.session.details.presentation.model

enum class SessionDetailFilterKind {
    Sort,
    Show,
    SessionType,
}

data class SessionDetailFilterOptionUi(val id: String, val label: String? = null)

data class SessionDetailFilterUiModel(
    val kind: SessionDetailFilterKind,
    val selectedId: String,
    val options: List<SessionDetailFilterOptionUi> = emptyList(),
)

data class SessionDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val header: SessionDetailHeaderUi = SessionDetailHeaderUi(),
    val stats: SessionDetailStatsUi = SessionDetailStatsUi(),
    val sortFilter: SessionDetailFilterUiModel = SessionDetailFilterUiModel(
        kind = SessionDetailFilterKind.Sort,
        selectedId = "lap",
        options = listOf(SessionDetailFilterOptionUi("lap")),
    ),
    val showFilter: SessionDetailFilterUiModel = SessionDetailFilterUiModel(
        kind = SessionDetailFilterKind.Show,
        selectedId = "all",
        options = listOf(SessionDetailFilterOptionUi("all")),
    ),
    val sessionTypeFilter: SessionDetailFilterUiModel = SessionDetailFilterUiModel(
        kind = SessionDetailFilterKind.SessionType,
        selectedId = "all_session_types",
        options = listOf(SessionDetailFilterOptionUi("all_session_types")),
    ),
    val page: Int = 1,
    val pageCount: Int = 1,
    val laps: List<SessionLapRowUi> = emptyList(),
    val visibleLaps: List<SessionLapRowUi> = emptyList(),
)
