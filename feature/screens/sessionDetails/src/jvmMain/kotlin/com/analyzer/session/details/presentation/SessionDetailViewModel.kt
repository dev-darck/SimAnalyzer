package com.analyzer.session.details.presentation

import com.analyzer.session.details.domain.model.SessionDetailPageResult
import com.analyzer.session.details.domain.usecase.SessionDetailCompareCriteria
import com.analyzer.session.details.domain.usecase.SessionDetailCompareSuggestionsUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailDataUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailImportCompareSessionUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailShareResultsUseCase
import com.analyzer.session.details.presentation.model.SessionDetailAction
import com.analyzer.session.details.presentation.model.SessionDetailCompareSessionPickerUi
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import com.analyzer.session.details.presentation.model.SessionDetailQueryUi
import com.analyzer.session.details.presentation.model.SessionDetailShareDialogUi
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.toDomain
import com.analyzer.session.details.presentation.model.toUi
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Inject
internal class SessionDetailViewModel(
    private val dataUseCase: SessionDetailDataUseCase,
    private val compareSuggestionsUseCase: SessionDetailCompareSuggestionsUseCase,
    private val importCompareSessionUseCase: SessionDetailImportCompareSessionUseCase,
    private val shareResultsUseCase: SessionDetailShareResultsUseCase,
) : LeakAwareMviViewModel<SessionDetailIntent, SessionDetailState>(SessionDetailState()) {

    private var query = SessionDetailQueryUi()
    private var currentSessionId: Long? = null
    private var loadedSessionId: Long? = null
    private var currentPageResult: SessionDetailPageResult? = null
    private var compareSelection = SessionDetailCompareSelection()
    private var compareSessionPicker = SessionDetailCompareSessionPickerUi()
    private var currentCompareCriteria: SessionDetailCompareCriteria? = null
    private var highlightedCompareSessionId: Long? = null
    private var shareDialog = SessionDetailShareDialogUi()
    private val _actions = MutableSharedFlow<SessionDetailAction>(extraBufferCapacity = 1)

    val actions = _actions.asSharedFlow()

    override suspend fun handleIntent(intent: SessionDetailIntent) {
        if (handleSessionIntent(intent)) return
        if (handleQueryIntent(intent)) return
        if (handleCompareIntent(intent)) return
        handleShareIntent(intent)
    }

    private suspend fun handleSessionIntent(intent: SessionDetailIntent): Boolean = when (intent) {
        is SessionDetailIntent.BindSession -> {
            bindSession(intent.sessionId)
            true
        }

        else -> false
    }

    private suspend fun handleQueryIntent(intent: SessionDetailIntent): Boolean = when (intent) {
        SessionDetailIntent.Refresh -> {
            reload(forceRefresh = true)
            true
        }

        is SessionDetailIntent.ChangeSort -> {
            updateQuery { copy(sortId = intent.optionId, page = 1) }
            true
        }

        is SessionDetailIntent.ChangeFilter -> {
            updateQuery { copy(showId = intent.optionId, page = 1) }
            true
        }

        is SessionDetailIntent.ChangeSessionTypeFilter -> {
            updateQuery {
                copy(
                    sessionTypeId = intent.optionId,
                    page = 1,
                )
            }
            true
        }

        is SessionDetailIntent.ChangePage -> {
            updateQuery { copy(page = intent.page) }
            true
        }

        else -> false
    }

    private suspend fun handleCompareIntent(intent: SessionDetailIntent): Boolean = when (intent) {
        SessionDetailIntent.StartCompareSelection -> {
            startCompareSelection()
            true
        }

        SessionDetailIntent.CancelCompareSelection -> {
            cancelCompareSelection()
            true
        }

        SessionDetailIntent.OpenCompareSessionPicker -> {
            openCompareSessionPicker()
            true
        }

        SessionDetailIntent.DismissCompareSessionPicker -> {
            dismissCompareSessionPicker()
            true
        }

        is SessionDetailIntent.ImportCompareSession -> {
            importCompareSession(intent.path)
            true
        }

        is SessionDetailIntent.ToggleCompareLap -> {
            toggleCompareLap(
                segmentId = intent.segmentId,
                lapNumber = intent.lapNumber,
            )
            true
        }

        else -> false
    }

    private suspend fun handleShareIntent(intent: SessionDetailIntent) {
        when (intent) {
            SessionDetailIntent.OpenShareResults -> openShareResultsDialog()
            SessionDetailIntent.DismissShareResults -> dismissShareResultsDialog()
            SessionDetailIntent.CopyShareResults -> copyShareResults()

            is SessionDetailIntent.ExportShareResultsToDirectory -> {
                exportShareResultsToDirectory(intent.directoryPath)
            }

            SessionDetailIntent.OpenShareSessionFiles -> openShareSessionFiles()
            else -> Unit
        }
    }

    private suspend fun bindSession(sessionId: Long) {
        if (currentSessionId == sessionId && loadedSessionId == sessionId) return
        currentSessionId = sessionId
        resetTransientUi()
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
                isCompareSelectionMode = compareSelection.isSelectionMode,
                selectedCompareLaps = compareSelection.selectedLaps,
                compareSessionPicker = compareSessionPicker,
                shareDialog = shareDialog,
            ),
        )
    }

    private fun startCompareSelection() {
        val nextSelection = compareSelection.start()
        if (nextSelection == compareSelection) return
        compareSelection = nextSelection
        renderCurrentPage()
    }

    private fun cancelCompareSelection() {
        val nextSelection = compareSelection.cancel()
        if (nextSelection == compareSelection) return
        compareSelection = nextSelection
        renderCurrentPage()
    }

    private suspend fun openCompareSessionPicker() {
        val sessionId = currentSessionId ?: return
        val criteria = currentPageResult?.page?.header?.toCompareCriteria(sessionId)
        if (criteria == null) {
            currentCompareCriteria = null
            highlightedCompareSessionId = null
            compareSessionPicker = missingCompareSessionPickerUi()
            renderCurrentPage()
            return
        }

        currentCompareCriteria = criteria
        highlightedCompareSessionId = null
        loadCompareSessionPicker(criteria = criteria)
    }

    private fun dismissCompareSessionPicker() {
        if (!compareSessionPicker.isVisible) return
        currentCompareCriteria = null
        highlightedCompareSessionId = null
        compareSessionPicker = SessionDetailCompareSessionPickerUi()
        renderCurrentPage()
    }

    private suspend fun importCompareSession(path: String) {
        val sessionId = currentSessionId ?: return
        val criteria = currentCompareCriteria
            ?: currentPageResult?.page?.header?.toCompareCriteria(sessionId)
            ?: return
        currentCompareCriteria = criteria
        compareSessionPicker = compareSessionPicker.copy(
            isVisible = true,
            isImporting = true,
            statusMessage = null,
        )
        renderCurrentPage()

        val feedback = importCompareSessionUseCase
            .importSession(criteria = criteria, path = path)
            .toCompareImportFeedback()
        highlightedCompareSessionId = feedback.highlightedSessionId
        if (feedback.shouldReloadSuggestions) {
            emitAction(feedback.action)
            loadCompareSessionPicker(
                criteria = criteria,
                forceRefresh = true,
                statusMessage = feedback.statusMessage,
            )
            return
        }
        compareSessionPicker = compareSessionPicker.copy(isImporting = false)
        renderCurrentPage()
        emitAction(feedback.action)
    }

    private fun openShareResultsDialog() {
        val sessionId = currentSessionId ?: return
        val page = currentPageResult?.page ?: return
        shareDialog = page.toShareDialogUi(sessionId = sessionId)
        renderCurrentPage()
    }

    private fun dismissShareResultsDialog() {
        if (!shareDialog.isVisible) return
        shareDialog = SessionDetailShareDialogUi()
        renderCurrentPage()
    }

    private fun toggleCompareLap(segmentId: Long, lapNumber: Int) {
        val nextSelection = compareSelection.toggle(
            visibleLaps = state.value.visibleLaps,
            segmentId = segmentId,
            lapNumber = lapNumber,
        )
        if (nextSelection == compareSelection) return
        compareSelection = nextSelection
        renderCurrentPage()
    }

    private suspend fun copyShareResults() {
        val summaryText = shareDialog.summaryText.takeIf(String::isNotBlank) ?: return
        emitAction(shareResultsUseCase.copySummary(summaryText).toCopySummaryAction())
    }

    private suspend fun exportShareResultsToDirectory(directoryPath: String) {
        val reportFileName = shareDialog.reportFileName.takeIf(String::isNotBlank) ?: return
        val summaryText = shareDialog.summaryText.takeIf(String::isNotBlank) ?: return
        emitAction(
            shareResultsUseCase
                .exportReport(directoryPath, reportFileName, summaryText)
                .toExportReportAction(),
        )
    }

    private suspend fun openShareSessionFiles() {
        val sessionId = currentSessionId ?: return
        emitAction(shareResultsUseCase.openSessionFiles(sessionId).toOpenSessionFilesAction())
    }

    private fun resetCompareSelection() {
        compareSelection = SessionDetailCompareSelection()
    }

    private fun resetTransientUi() {
        resetCompareSelection()
        compareSessionPicker = SessionDetailCompareSessionPickerUi()
        currentCompareCriteria = null
        highlightedCompareSessionId = null
        shareDialog = SessionDetailShareDialogUi()
    }

    private suspend fun loadCompareSessionPicker(
        criteria: SessionDetailCompareCriteria,
        forceRefresh: Boolean = false,
        statusMessage: String? = null,
    ) {
        compareSessionPicker = criteria.toLoadingCompareSessionPickerUi(statusMessage)
        renderCurrentPage()

        val suggestions = compareSuggestionsUseCase.loadSuggestions(
            criteria = criteria,
            forceRefresh = forceRefresh,
        )
        compareSessionPicker = criteria.toReadyCompareSessionPickerUi(
            suggestions = suggestions,
            highlightedSessionId = highlightedCompareSessionId,
            statusMessage = statusMessage,
        )
        renderCurrentPage()
    }

    private suspend fun emitAction(action: SessionDetailAction?) {
        if (action != null) _actions.emit(action)
    }
}
