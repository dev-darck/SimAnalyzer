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
    data object OpenCompareSessionPicker : SessionDetailIntent
    data object DismissCompareSessionPicker : SessionDetailIntent
    data class ImportCompareSession(val path: String) : SessionDetailIntent
    data object OpenShareResults : SessionDetailIntent
    data object DismissShareResults : SessionDetailIntent
    data object CopyShareResults : SessionDetailIntent
    data class ExportShareResultsToDirectory(val directoryPath: String) : SessionDetailIntent
    data object OpenShareSessionFiles : SessionDetailIntent
    data class ToggleCompareLap(val segmentId: Long, val lapNumber: Int) : SessionDetailIntent
}
