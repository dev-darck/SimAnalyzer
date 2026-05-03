package com.analyzer.session.details.presentation.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
    val compareSessionPicker: SessionDetailCompareSessionPickerUi = SessionDetailCompareSessionPickerUi(),
    val shareDialog: SessionDetailShareDialogUi = SessionDetailShareDialogUi(),
)
