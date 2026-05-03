package com.analyzer.session.details.presentation

import com.analyzer.session.details.domain.model.SessionDetailPageResult
import com.analyzer.session.details.domain.usecase.SessionDetailDataUseCase
import com.analyzer.session.details.presentation.model.SessionDetailCompareLapUi
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import com.analyzer.session.details.presentation.model.SessionDetailQueryUi
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.toDomain
import com.analyzer.session.details.presentation.model.toUi
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Inject
internal class SessionDetailViewModel(private val dataUseCase: SessionDetailDataUseCase) :
    LeakAwareMviViewModel<SessionDetailIntent, SessionDetailState>(SessionDetailState()) {

    private var query = SessionDetailQueryUi()
    private var currentSessionId: Long? = null
    private var loadedSessionId: Long? = null
    private var currentPageResult: SessionDetailPageResult? = null
    private var isCompareSelectionMode = false
    private var selectedCompareLaps: PersistentList<SessionDetailCompareLapUi> = persistentListOf()

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

            SessionDetailIntent.StartCompareSelection -> startCompareSelection()

            SessionDetailIntent.CancelCompareSelection -> cancelCompareSelection()

            is SessionDetailIntent.ToggleCompareLap -> toggleCompareLap(
                segmentId = intent.segmentId,
                lapNumber = intent.lapNumber,
            )
        }
    }

    private suspend fun bindSession(sessionId: Long) {
        if (currentSessionId == sessionId && loadedSessionId == sessionId) return
        currentSessionId = sessionId
        resetCompareSelection()
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
            query = query.copy(page = 1).toDomain(),
            forceRefresh = forceRefresh,
        )
        if (result == null) {
            loadedSessionId = sessionId
            currentPageResult = null
            setState(SessionDetailState(isLoading = false, error = "Session data not found."))
            return
        }
        loadedSessionId = sessionId
        applyResult(result)
    }

    private suspend fun updateQuery(mutator: SessionDetailQueryUi.() -> SessionDetailQueryUi) {
        val sessionId = currentSessionId ?: return
        val nextQuery = mutator(query)
        val result = dataUseCase.loadPage(sessionId, nextQuery.toDomain()) ?: return
        applyResult(result)
    }

    private fun applyResult(result: SessionDetailPageResult) {
        currentPageResult = result
        query = result.query.toUi()
        renderCurrentPage()
    }

    private fun renderCurrentPage() {
        val result = currentPageResult ?: return
        setState(
            result.page.toSessionDetailState(
                query = query,
                isCompareSelectionMode = isCompareSelectionMode,
                selectedCompareLaps = selectedCompareLaps,
            ),
        )
    }

    private fun startCompareSelection() {
        if (isCompareSelectionMode) return
        isCompareSelectionMode = true
        renderCurrentPage()
    }

    private fun cancelCompareSelection() {
        if (!isCompareSelectionMode && selectedCompareLaps.isEmpty()) return
        resetCompareSelection()
        renderCurrentPage()
    }

    private fun toggleCompareLap(segmentId: Long, lapNumber: Int) {
        if (!isCompareSelectionMode) return
        val lap = state.value.visibleLaps.firstOrNull { row ->
            row.segmentId == segmentId && row.lapNumber == lapNumber
        } ?: return
        val existingIndex = selectedCompareLaps.indexOfFirst { selected ->
            selected.segmentId == segmentId && selected.lapNumber == lapNumber
        }
        selectedCompareLaps = when {
            existingIndex >= 0 -> selectedCompareLaps.removeAt(existingIndex)

            selectedCompareLaps.size >= 2 -> selectedCompareLaps

            selectedCompareLaps.isNotEmpty() && selectedCompareLaps.first().segmentId != segmentId -> {
                selectedCompareLaps
            }

            else -> {
                selectedCompareLaps.add(
                    SessionDetailCompareLapUi(
                        segmentId = lap.segmentId,
                        lapNumber = lap.lapNumber,
                        lapLabel = lap.lapLabel,
                        sessionTypeLabel = lap.sessionTypeLabel,
                        totalTimeMs = lap.totalTimeMs,
                    ),
                )
            }
        }
        renderCurrentPage()
    }

    private fun resetCompareSelection() {
        isCompareSelectionMode = false
        selectedCompareLaps = persistentListOf()
    }
}
