package com.analyzer.session.details.presentation.model

sealed interface SessionDetailIntent {
    data class BindSession(val sessionId: Long) : SessionDetailIntent
    data object Refresh : SessionDetailIntent
    data class ChangeSort(val optionId: String) : SessionDetailIntent
    data class ChangeFilter(val optionId: String) : SessionDetailIntent
    data class ChangeSessionTypeFilter(val optionId: String) : SessionDetailIntent
    data class ChangePage(val page: Int) : SessionDetailIntent
    data object StartCompareSelection : SessionDetailIntent
    data object CancelCompareSelection : SessionDetailIntent
    data class ToggleCompareLap(val segmentId: Long, val lapNumber: Int) : SessionDetailIntent
}
