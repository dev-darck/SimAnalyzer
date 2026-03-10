package com.analyzer.session.presentation

import com.analyzer.session.domain.model.SessionListQuery
import com.analyzer.session.domain.usecase.SessionListDataUseCase
import com.analyzer.session.domain.usecase.SessionTrackMapUseCase
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import com.project.analyzer.ui.components.TrackMapData
import dev.zacsweers.metro.Inject

@Inject
internal class SessionListViewModel(
    private val dataUseCase: SessionListDataUseCase,
    private val sessionTrackMapUseCase: SessionTrackMapUseCase,
) : LeakAwareMviViewModel<SessionListIntent, SessionListState>(SessionListState()) {

    private var query = SessionListQuery()
    private var trackMapsByKey: Map<String, TrackMapData> = emptyMap()
    private var hasLoadedOnce = false

    override suspend fun handleIntent(intent: SessionListIntent) {
        when (intent) {
            SessionListIntent.Start -> start()
            SessionListIntent.Refresh -> refresh(forceRefresh = true)
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

    private suspend fun refresh(showLoading: Boolean = true, forceRefresh: Boolean = false) {
        if (showLoading) {
            updateState { copy(isLoading = true, error = null) }
        } else {
            updateState { copy(error = null) }
        }
        loadPage(
            nextQuery = query,
            forceRefresh = forceRefresh,
        )
    }

    private suspend fun updateQuery(mutator: SessionListQuery.() -> SessionListQuery) {
        loadPage(mutator(query))
    }

    private suspend fun saveSession(sessionId: Long) {
        val saved = dataUseCase.saveSession(sessionId)
        if (saved) {
            refresh(showLoading = false, forceRefresh = true)
        } else {
            updateState { copy(error = "Failed to save session.") }
        }
    }

    private suspend fun deleteSession(sessionId: Long) {
        val deleted = dataUseCase.deleteSession(sessionId)
        if (deleted) {
            refresh(showLoading = false, forceRefresh = true)
        } else {
            updateState { copy(error = "Failed to delete session.") }
        }
    }

    private suspend fun loadPage(nextQuery: SessionListQuery, forceRefresh: Boolean = false) {
        val result = dataUseCase.loadPage(
            query = nextQuery,
            forceRefresh = forceRefresh,
        )
        trackMapsByKey = sessionTrackMapUseCase.loadTrackMaps(result.page.rows)
        query = result.query
        hasLoadedOnce = true
        val mapped = result.page.toSessionListState(
            query = result.query,
        )
        setState(
            mapped.copy(visibleSessions = mapped.visibleSessions.map(::mapTrackMap)),
        )
    }

    private fun mapTrackMap(row: SessionRowUi): SessionRowUi = row.copy(
        trackMap = sessionTrackMapUseCase.resolveTrackMap(
            trackMapsByKey = trackMapsByKey,
            gameId = row.gameId,
            trackId = row.trackId,
            layoutId = row.layoutId,
        ),
    )
}
