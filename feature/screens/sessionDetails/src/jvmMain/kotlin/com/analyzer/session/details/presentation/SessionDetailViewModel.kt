package com.analyzer.session.details.presentation

import com.analyzer.session.details.domain.interactor.SessionDetailManager
import com.analyzer.session.details.domain.model.SessionDetailDataset
import com.analyzer.session.details.domain.model.SessionDetailQuery
import com.analyzer.session.details.domain.model.SessionDetailResult
import com.analyzer.session.details.presentation.mapper.SessionDetailUiStateMapper
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject

@Inject
internal class SessionDetailViewModel(
    private val manager: SessionDetailManager,
    private val uiStateMapper: SessionDetailUiStateMapper,
) : LeakAwareMviViewModel<SessionDetailIntent, SessionDetailState>(SessionDetailState()) {

    private var dataset = SessionDetailDataset()
    private var query = SessionDetailQuery()
    private var currentSessionId: Long? = null
    private var loadedSessionId: Long? = null

    override suspend fun handleIntent(intent: SessionDetailIntent) {
        when (intent) {
            is SessionDetailIntent.BindSession -> bindSession(intent.sessionId)

            SessionDetailIntent.Refresh -> reload()

            is SessionDetailIntent.ChangeSort -> updateQuery { copy(sortId = intent.optionId, page = 1) }

            is SessionDetailIntent.ChangeFilter -> updateQuery { copy(showId = intent.optionId, page = 1) }

            is SessionDetailIntent.ChangeSessionTypeFilter -> updateQuery {
                copy(
                    sessionTypeId = intent.optionId,
                    page = 1,
                )
            }

            is SessionDetailIntent.ChangePage -> updateQuery { copy(page = intent.page) }
        }
    }

    private suspend fun bindSession(sessionId: Long) {
        if (currentSessionId == sessionId && loadedSessionId == sessionId) return
        currentSessionId = sessionId
        load(sessionId)
    }

    private suspend fun reload() {
        val sessionId = currentSessionId ?: return
        load(sessionId)
    }

    private suspend fun load(sessionId: Long) {
        if (loadedSessionId == null) {
            updateState { copy(isLoading = true, error = null) }
        } else {
            updateState { copy(error = null) }
        }
        val result = manager.load(sessionId = sessionId, query = query.copy(page = 1))
        if (result == null) {
            dataset = SessionDetailDataset()
            loadedSessionId = sessionId
            applyResult(
                result = manager.project(dataset = dataset, query = query.copy(page = 1)),
                errorOverride = "Session data not found.",
            )
            return
        }
        loadedSessionId = sessionId
        applyResult(result)
    }

    private suspend fun updateQuery(mutator: SessionDetailQuery.() -> SessionDetailQuery) {
        applyResult(manager.project(dataset, mutator(query)))
    }

    private fun applyResult(result: SessionDetailResult, errorOverride: String? = null) {
        dataset = result.dataset
        query = result.query
        setState(
            uiStateMapper.map(
                dataset = result.dataset,
                projection = result.projection,
                query = result.query,
                errorOverride = errorOverride,
            ),
        )
    }
}
