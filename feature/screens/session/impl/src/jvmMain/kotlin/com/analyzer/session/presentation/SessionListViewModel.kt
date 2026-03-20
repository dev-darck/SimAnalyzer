package com.analyzer.session.presentation

import androidx.lifecycle.viewModelScope
import com.analyzer.session.domain.model.SessionListQuery
import com.analyzer.session.domain.usecase.SessionListDataUseCase
import com.analyzer.session.domain.usecase.SessionTrackMapIdentity
import com.analyzer.session.domain.usecase.SessionTrackMapUseCase
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import com.project.analyzer.ui.components.TrackMapData
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Inject
internal class SessionListViewModel(
    private val dataUseCase: SessionListDataUseCase,
    private val sessionTrackMapUseCase: SessionTrackMapUseCase,
) : LeakAwareMviViewModel<SessionListIntent, SessionListState>(SessionListState()) {

    private var query = SessionListQuery()
    private var trackMapsByKey: Map<String, TrackMapData> = emptyMap()
    private var hasLoadedOnce = false
    private var trackMapLoadJob: Job? = null
    private var trackMapLoadRequestId: Long = 0L
    private var searchJob: Job? = null

    override suspend fun handleIntent(intent: SessionListIntent) {
        when (intent) {
            SessionListIntent.Start -> start()
            SessionListIntent.Refresh -> refresh(forceRefresh = true)
            is SessionListIntent.ChangeGame -> updateQuery { copy(gameId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeTrack -> updateQuery { copy(trackId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeCar -> updateQuery { copy(carId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeDate -> updateQuery { copy(dateId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeSort -> updateQuery { copy(sortId = intent.optionId, page = 1) }
            is SessionListIntent.ChangeSearch -> updateSearchQuery(intent.query)
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
        cancelPendingSearch()
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
        cancelPendingSearch()
        loadPage(mutator(query))
    }

    private suspend fun updateSearchQuery(searchQuery: String) {
        val nextQuery = query.copy(searchQuery = searchQuery, page = 1)
        if (nextQuery == query) return

        query = nextQuery
        cancelPendingSearch()
        updateState {
            copy(
                searchQuery = searchQuery,
                page = 1,
                error = null,
            )
        }
        searchJob = viewModelScope.launch {
            delay(250)
            loadPage(nextQuery)
        }
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
        query = result.query
        hasLoadedOnce = true
        val mapped = result.page.toSessionListState(
            query = result.query,
        )
        setState(mapped.applyTrackMaps(trackMapsByKey))
        enqueueTrackMapLoad(mapped.visibleSessions)
    }

    private fun enqueueTrackMapLoad(rows: List<SessionRowUi>) {
        trackMapLoadJob?.cancel()
        val requestId = ++trackMapLoadRequestId
        val pendingRows = rows.filterNot(::hasCachedTrackMap)
        if (pendingRows.isEmpty()) return
        trackMapLoadJob = viewModelScope.launch {
            val loadedTrackMaps = sessionTrackMapUseCase.loadTrackMaps(
                items = pendingRows.map { it.toTrackMapIdentity() },
            )
            if (loadedTrackMaps.isEmpty() || requestId != trackMapLoadRequestId) return@launch
            trackMapsByKey = trackMapsByKey + loadedTrackMaps
            updateState { applyTrackMaps(trackMapsByKey) }
        }
    }

    private fun hasCachedTrackMap(row: SessionRowUi): Boolean = sessionTrackMapUseCase.resolveTrackMap(
        trackMapsByKey = trackMapsByKey,
        gameId = row.gameId,
        trackId = row.trackId,
        layoutId = row.layoutId,
    ) != null

    private fun SessionRowUi.toTrackMapIdentity() = SessionTrackMapIdentity(
        gameId = gameId,
        trackId = trackId,
        layoutId = layoutId,
    )

    private fun SessionListState.applyTrackMaps(trackMapsByKey: Map<String, TrackMapData>): SessionListState {
        if (trackMapsByKey.isEmpty() || visibleSessions.isEmpty()) return this

        var changed = false
        val updatedRows = visibleSessions.map { row ->
            val resolvedTrackMap = sessionTrackMapUseCase.resolveTrackMap(
                trackMapsByKey = trackMapsByKey,
                gameId = row.gameId,
                trackId = row.trackId,
                layoutId = row.layoutId,
            )
            if (row.trackMap === resolvedTrackMap) {
                row
            } else {
                changed = true
                row.copy(trackMap = resolvedTrackMap)
            }
        }

        if (!changed) return this
        return copy(visibleSessions = updatedRows.toImmutableList())
    }

    private fun cancelPendingSearch() {
        searchJob?.cancel()
        searchJob = null
    }
}
