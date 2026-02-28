package com.analyzer.session.presentation.model

sealed interface SessionListIntent {
    data object Start : SessionListIntent
    data object Refresh : SessionListIntent
    data class ChangeGame(val optionId: String) : SessionListIntent
    data class ChangeTrack(val optionId: String) : SessionListIntent
    data class ChangeCar(val optionId: String) : SessionListIntent
    data class ChangeDate(val optionId: String) : SessionListIntent
    data class ChangeSort(val optionId: String) : SessionListIntent
    data class ChangeSearch(val query: String) : SessionListIntent
    data class ChangePage(val page: Int) : SessionListIntent
    data class SaveSession(val sessionId: Long) : SessionListIntent
    data class DeleteSession(val sessionId: Long) : SessionListIntent
}
