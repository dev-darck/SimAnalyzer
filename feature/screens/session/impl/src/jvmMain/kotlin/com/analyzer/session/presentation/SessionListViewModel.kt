package com.analyzer.session.presentation

import com.analyzer.session.domain.interactor.SessionListManager
import com.analyzer.session.domain.model.SessionListDataset
import com.analyzer.session.domain.model.SessionListQuery
import com.analyzer.session.domain.model.SessionListResult
import com.analyzer.session.presentation.mapper.SessionListUiStateMapper
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject

@Inject
internal class SessionListViewModel(
    private val manager: SessionListManager,
    private val uiStateMapper: SessionListUiStateMapper,
) : LeakAwareMviViewModel<SessionListIntent, SessionListState>(SessionListState()) {

    private var dataset = SessionListDataset()
    private var query = SessionListQuery()
    private var hasLoadedOnce = false

    override suspend fun handleIntent(intent: SessionListIntent) {
        when (intent) {
            SessionListIntent.Start -> start()
            SessionListIntent.Refresh -> refresh()
            is SessionListIntent.ChangeGame -> updateQuery { copy(gameId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeTrack -> updateQuery { copy(trackId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeCar -> updateQuery { copy(carId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeDate -> updateQuery { copy(dateId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeSort -> updateQuery { copy(sortId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeSearch -> updateQuery { copy(searchQuery = intent.query, page = 1) }
            is SessionListIntent.ChangePage -> updateQuery { copy(page = intent.page) }
            is SessionListIntent.SaveSession -> saveSession(intent.sessionId)
            is SessionListIntent.DeleteSession -> deleteSession(intent.sessionId)
        }
    }

    private suspend fun start() {
        if (hasLoadedOnce) return
        refresh()
    }

    private suspend fun refresh(showLoading: Boolean = true) {
        if (showLoading) {
            updateState { copy(isLoading = true, error = null) }
        } else {
            updateState { copy(error = null) }
        }
        applyInteractorResult(manager.refresh(query))
    }

    private suspend fun updateQuery(mutator: SessionListQuery.() -> SessionListQuery) {
        applyInteractorResult(manager.project(dataset, mutator(query)))
    }

    private suspend fun saveSession(sessionId: Long) {
        val saved = manager.saveSession(sessionId)
        if (saved) {
            refresh(showLoading = false)
        } else {
            updateState { copy(error = "Failed to save session.") }
        }
    }

    private suspend fun deleteSession(sessionId: Long) {
        val deleted = manager.deleteSession(sessionId)
        if (deleted) {
            refresh(showLoading = false)
        } else {
            updateState { copy(error = "Failed to delete session.") }
        }
    }

    private fun applyInteractorResult(result: SessionListResult) {
        dataset = result.dataset
        query = result.query
        hasLoadedOnce = true
        setState(
            uiStateMapper.map(
                dataset = result.dataset,
                projection = result.projection,
                query = result.query,
            ),
        )
    }
}
