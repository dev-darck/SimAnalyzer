package com.analyzer.session.details.presentation.model

sealed interface SessionDetailIntent {
    data object Refresh : SessionDetailIntent
    data class ChangeSort(val optionId: String) : SessionDetailIntent
    data class ChangeFilter(val optionId: String) : SessionDetailIntent
    data class ChangePage(val page: Int) : SessionDetailIntent
}
