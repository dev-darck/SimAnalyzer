package com.analyzer.session.details.presentation

import com.analyzer.session.details.domain.model.SessionDetailQuery
import com.analyzer.session.details.domain.usecase.SessionDetailDataUseCase
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject

@Inject
internal class SessionDetailViewModel(private val dataUseCase: SessionDetailDataUseCase) :
    LeakAwareMviViewModel<SessionDetailIntent, SessionDetailState>(SessionDetailState()) {

    private var query = SessionDetailQuery()
    private var currentSessionId: Long? = null
    private var loadedSessionId: Long? = null

    override suspend fun handleIntent(intent: SessionDetailIntent) {
        when (intent) {
            is SessionDetailIntent.BindSession -> bindSession(intent.sessionId)

            SessionDetailIntent.Refresh -> reload(forceRefresh = true)

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
        load(sessionId = sessionId)
    }

    private suspend fun reload(forceRefresh: Boolean = false) {
        val sessionId = currentSessionId ?: return
        load(
            sessionId = sessionId,
            forceRefresh = forceRefresh,
        )
    }

    private suspend fun load(sessionId: Long, forceRefresh: Boolean = false) {
        if (loadedSessionId == null) {
            updateState { copy(isLoading = true, error = null) }
        } else {
            updateState { copy(error = null) }
        }
        val result = dataUseCase.loadPage(
            sessionId = sessionId,
            query = query.copy(page = 1),
            forceRefresh = forceRefresh,
        )
        if (result == null) {
            loadedSessionId = sessionId
            setState(SessionDetailState(isLoading = false, error = "Session data not found."))
            return
        }
        loadedSessionId = sessionId
        applyResult(result)
    }

    private suspend fun updateQuery(mutator: SessionDetailQuery.() -> SessionDetailQuery) {
        val sessionId = currentSessionId ?: return
        val result = dataUseCase.loadPage(sessionId, mutator(query)) ?: return
        applyResult(result)
    }

    private fun applyResult(result: com.analyzer.session.details.domain.model.SessionDetailPageResult) {
        query = result.query
        setState(
            result.page.toSessionDetailState(
                query = result.query,
            ),
        )
    }
}
