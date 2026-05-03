package com.analyzer.session.details.presentation.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

enum class SessionDetailFilterKind {
    Sort,
    Show,
    SessionType,
}

data class SessionDetailFilterOptionUi(val id: String, val label: String? = null)

data class SessionDetailFilterUiModel(
    val kind: SessionDetailFilterKind,
    val selectedId: String,
    val options: ImmutableList<SessionDetailFilterOptionUi> = persistentListOf(),
)

data class SessionDetailCompareLapUi(
    val segmentId: Long,
    val lapNumber: Int,
    val lapLabel: String,
    val sessionTypeLabel: String,
    val totalTimeMs: Int?,
)

data class SessionDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val header: SessionDetailHeaderUi = SessionDetailHeaderUi(),
    val stats: SessionDetailStatsUi = SessionDetailStatsUi(),
    val sortFilter: SessionDetailFilterUiModel = SessionDetailFilterUiModel(
        kind = SessionDetailFilterKind.Sort,
        selectedId = "lap",
        options = persistentListOf(SessionDetailFilterOptionUi("lap")),
    ),
    val showFilter: SessionDetailFilterUiModel = SessionDetailFilterUiModel(
        kind = SessionDetailFilterKind.Show,
        selectedId = "all",
        options = persistentListOf(SessionDetailFilterOptionUi("all")),
    ),
    val sessionTypeFilter: SessionDetailFilterUiModel = SessionDetailFilterUiModel(
        kind = SessionDetailFilterKind.SessionType,
        selectedId = "all_session_types",
        options = persistentListOf(SessionDetailFilterOptionUi("all_session_types")),
    ),
    val page: Int = 1,
    val pageCount: Int = 1,
    val visibleLaps: ImmutableList<SessionLapRowUi> = persistentListOf(),
    val isCompareSelectionMode: Boolean = false,
    val selectedCompareLaps: ImmutableList<SessionDetailCompareLapUi> = persistentListOf(),
    val compareConfirmEnabled: Boolean = false,
)
