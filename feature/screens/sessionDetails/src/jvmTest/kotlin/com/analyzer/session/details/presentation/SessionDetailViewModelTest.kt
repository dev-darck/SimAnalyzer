@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.analyzer.session.details.presentation

import com.analyzer.session.details.domain.model.SESSION_DETAIL_TYPE_ALL
import com.analyzer.session.details.domain.model.SessionDetailDomainHeader
import com.analyzer.session.details.domain.model.SessionDetailDomainStats
import com.analyzer.session.details.domain.model.SessionDetailPage
import com.analyzer.session.details.domain.model.SessionDetailPageResult
import com.analyzer.session.details.domain.model.SessionDetailQuery
import com.analyzer.session.details.domain.model.SessionLapDomainItem
import com.analyzer.session.details.domain.model.SessionLapDomainStatus
import com.analyzer.session.details.domain.usecase.SessionDetailCompareCriteria
import com.analyzer.session.details.domain.usecase.SessionDetailCompareSuggestion
import com.analyzer.session.details.domain.usecase.SessionDetailCompareSuggestions
import com.analyzer.session.details.domain.usecase.SessionDetailCompareSuggestionsUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailDataUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailImportCompareSessionResult
import com.analyzer.session.details.domain.usecase.SessionDetailImportCompareSessionUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailShareResults
import com.analyzer.session.details.domain.usecase.SessionDetailShareResultsUseCase
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionDetailViewModelTest {

    @Test
    fun `compare mode selects two laps from the same segment`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = buildViewModel()

            viewModel.dispatch(SessionDetailIntent.BindSession(sessionId = 42L))
            advanceUntilIdle()
            viewModel.dispatch(SessionDetailIntent.StartCompareSelection)
            advanceUntilIdle()
            viewModel.dispatch(SessionDetailIntent.ToggleCompareLap(segmentId = 101L, lapNumber = 1))
            advanceUntilIdle()

            val afterFirstSelection = viewModel.state.value
            assertTrue(afterFirstSelection.isCompareSelectionMode)
            assertEquals(listOf(1), afterFirstSelection.selectedCompareLaps.map { it.lapNumber })
            assertTrue(afterFirstSelection.visibleLaps[0].compareSelected)
            assertTrue(afterFirstSelection.visibleLaps[1].compareAvailable)
            assertTrue(afterFirstSelection.visibleLaps[2].compareAvailable)
            assertFalse(afterFirstSelection.visibleLaps[3].compareAvailable)

            viewModel.dispatch(SessionDetailIntent.ToggleCompareLap(segmentId = 101L, lapNumber = 2))
            advanceUntilIdle()

            val finalState = viewModel.state.value
            assertTrue(finalState.compareConfirmEnabled)
            assertEquals(listOf(1, 2), finalState.selectedCompareLaps.map { it.lapNumber })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `compare mode ignores third lap and clears selection on cancel`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = buildViewModel()

            viewModel.dispatch(SessionDetailIntent.BindSession(sessionId = 42L))
            advanceUntilIdle()
            viewModel.dispatch(SessionDetailIntent.StartCompareSelection)
            advanceUntilIdle()
            viewModel.dispatch(SessionDetailIntent.ToggleCompareLap(segmentId = 101L, lapNumber = 1))
            viewModel.dispatch(SessionDetailIntent.ToggleCompareLap(segmentId = 101L, lapNumber = 2))
            advanceUntilIdle()

            viewModel.dispatch(SessionDetailIntent.ToggleCompareLap(segmentId = 101L, lapNumber = 3))
            viewModel.dispatch(SessionDetailIntent.ToggleCompareLap(segmentId = 202L, lapNumber = 1))
            advanceUntilIdle()

            val lockedState = viewModel.state.value
            assertEquals(listOf(1, 2), lockedState.selectedCompareLaps.map { it.lapNumber })

            viewModel.dispatch(SessionDetailIntent.CancelCompareSelection)
            advanceUntilIdle()

            val resetState = viewModel.state.value
            assertFalse(resetState.isCompareSelectionMode)
            assertFalse(resetState.compareConfirmEnabled)
            assertTrue(resetState.selectedCompareLaps.isEmpty())
            assertTrue(resetState.visibleLaps.none { it.compareSelected })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `compare session picker loads suggested sessions`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val compareSuggestionsUseCase = FakeSessionDetailCompareSuggestionsUseCase()
            val viewModel = buildViewModel(compareSuggestionsUseCase = compareSuggestionsUseCase)

            viewModel.dispatch(SessionDetailIntent.BindSession(sessionId = 42L))
            advanceUntilIdle()
            viewModel.dispatch(SessionDetailIntent.OpenCompareSessionPicker)
            advanceUntilIdle()

            val picker = viewModel.state.value.compareSessionPicker
            assertTrue(picker.isVisible)
            assertFalse(picker.isLoading)
            assertEquals(1, picker.candidates.size)
            assertEquals(77L, picker.candidates.single().sessionId)
            assertEquals("Monza GP", compareSuggestionsUseCase.lastCriteria?.trackLabel)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `share dialog builds summary and copies it`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val shareResultsUseCase = FakeSessionDetailShareResultsUseCase()
            val viewModel = buildViewModel(shareResultsUseCase = shareResultsUseCase)

            viewModel.dispatch(SessionDetailIntent.BindSession(sessionId = 42L))
            advanceUntilIdle()
            viewModel.dispatch(SessionDetailIntent.OpenShareResults)
            advanceUntilIdle()

            val shareDialog = viewModel.state.value.shareDialog
            assertTrue(shareDialog.isVisible)
            assertTrue(shareDialog.summaryText.contains("Monza GP"))
            assertTrue(shareDialog.reportFileName.endsWith(".md"))

            val actionDeferred = async { viewModel.actions.first() }
            viewModel.dispatch(SessionDetailIntent.CopyShareResults)
            advanceUntilIdle()

            assertEquals(shareDialog.summaryText, shareResultsUseCase.lastCopiedSummary)
            assertEquals("Summary copied", actionDeferred.await().title)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `import compare session refreshes suggestions and highlights the added session`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val compareSuggestionsUseCase = FakeSessionDetailCompareSuggestionsUseCase()
            val importCompareSessionUseCase = FakeSessionDetailImportCompareSessionUseCase()
            val viewModel = buildViewModel(
                compareSuggestionsUseCase = compareSuggestionsUseCase,
                importCompareSessionUseCase = importCompareSessionUseCase,
            )

            viewModel.dispatch(SessionDetailIntent.BindSession(sessionId = 42L))
            advanceUntilIdle()
            viewModel.dispatch(SessionDetailIntent.OpenCompareSessionPicker)
            advanceUntilIdle()

            val actionDeferred = async { viewModel.actions.first() }
            viewModel.dispatch(SessionDetailIntent.ImportCompareSession("C:/temp/session"))
            advanceUntilIdle()

            assertEquals("C:/temp/session", importCompareSessionUseCase.lastPath)
            assertEquals(listOf(false, true), compareSuggestionsUseCase.forceRefreshRequests)
            assertEquals("Compare session added", actionDeferred.await().title)
            assertEquals("Just added", viewModel.state.value.compareSessionPicker.candidates.single().recommendationLabel)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private fun buildViewModel(
    dataUseCase: SessionDetailDataUseCase = FakeSessionDetailDataUseCase(),
    compareSuggestionsUseCase: SessionDetailCompareSuggestionsUseCase = FakeSessionDetailCompareSuggestionsUseCase(),
    importCompareSessionUseCase: SessionDetailImportCompareSessionUseCase = FakeSessionDetailImportCompareSessionUseCase(),
    shareResultsUseCase: SessionDetailShareResultsUseCase = FakeSessionDetailShareResultsUseCase(),
): SessionDetailViewModel = SessionDetailViewModel(
    dataUseCase = dataUseCase,
    compareSuggestionsUseCase = compareSuggestionsUseCase,
    importCompareSessionUseCase = importCompareSessionUseCase,
    shareResultsUseCase = shareResultsUseCase,
)

private class FakeSessionDetailDataUseCase : SessionDetailDataUseCase {

    override suspend fun loadPage(
        sessionId: Long,
        query: SessionDetailQuery,
        forceRefresh: Boolean,
    ): SessionDetailPageResult = SessionDetailPageResult(
        query = query,
        page = SessionDetailPage(
            header = SessionDetailDomainHeader(
                carLabel = "BMW M4 GT3",
                trackLabel = "Monza GP",
                sessionTypeLabel = "Race",
                gameId = "acc",
                trackId = "monza",
                layoutId = "gp",
                carModel = "bmw_m4_gt3",
            ),
            stats = SessionDetailDomainStats(
                bestLapLabel = "1:38.100",
                averageLapLabel = "1:39.200",
                incidentsCount = 0,
            ),
            defaultSessionTypeId = SESSION_DETAIL_TYPE_ALL,
            laps = listOf(
                lap(segmentId = 101L, lapNumber = 1, totalTimeMs = 99_800, totalTime = "1:39.800"),
                lap(segmentId = 101L, lapNumber = 2, totalTimeMs = 98_100, totalTime = "1:38.100"),
                lap(segmentId = 101L, lapNumber = 3, totalTimeMs = 99_000, totalTime = "1:39.000"),
                lap(segmentId = 202L, lapNumber = 1, totalTimeMs = 101_000, totalTime = "1:41.000"),
            ),
            page = query.page,
            pageCount = 1,
            error = null,
        ),
    )
}

private class FakeSessionDetailCompareSuggestionsUseCase : SessionDetailCompareSuggestionsUseCase {

    var lastCriteria: SessionDetailCompareCriteria? = null
    val forceRefreshRequests = mutableListOf<Boolean>()

    override suspend fun loadSuggestions(
        criteria: SessionDetailCompareCriteria,
        forceRefresh: Boolean,
    ): SessionDetailCompareSuggestions {
        lastCriteria = criteria
        forceRefreshRequests += forceRefresh
        return SessionDetailCompareSuggestions(
            candidates = listOf(
                SessionDetailCompareSuggestion(
                    sessionId = 77L,
                    carLabel = "BMW M4 GT3",
                    sessionTypeLabel = "Race",
                    dateLabel = "Mar 20, 2026",
                    timeLabel = "20:40",
                    bestLapLabel = "1:38.100",
                    lapsLabel = "12",
                    recommendationLabel = "Same car",
                ),
            ),
        )
    }
}

private class FakeSessionDetailImportCompareSessionUseCase : SessionDetailImportCompareSessionUseCase {

    var lastCriteria: SessionDetailCompareCriteria? = null
    var lastPath: String? = null
    var result: SessionDetailImportCompareSessionResult =
        SessionDetailImportCompareSessionResult.Imported(
            sessionId = 77L,
            destinationPath = "C:/recordings/77",
        )

    override suspend fun importSession(
        criteria: SessionDetailCompareCriteria,
        path: String,
    ): SessionDetailImportCompareSessionResult {
        lastCriteria = criteria
        lastPath = path
        return result
    }
}

private class FakeSessionDetailShareResultsUseCase : SessionDetailShareResultsUseCase {

    var lastCopiedSummary: String? = null
    var lastExportDirectoryPath: String? = null
    var lastReportFileName: String? = null
    var lastOpenSessionId: Long? = null

    override suspend fun copySummary(summaryText: String): SessionDetailShareResults {
        lastCopiedSummary = summaryText
        return SessionDetailShareResults.CopiedSummary
    }

    override suspend fun exportReport(
        directoryPath: String,
        reportFileName: String,
        summaryText: String,
    ): SessionDetailShareResults {
        lastExportDirectoryPath = directoryPath
        lastReportFileName = reportFileName
        lastCopiedSummary = summaryText
        return SessionDetailShareResults.ExportedReport("$directoryPath/$reportFileName")
    }

    override suspend fun openSessionFiles(sessionId: Long): SessionDetailShareResults {
        lastOpenSessionId = sessionId
        return SessionDetailShareResults.OpenedSessionFiles
    }
}

private fun lap(
    segmentId: Long,
    lapNumber: Int,
    totalTimeMs: Int,
    totalTime: String,
): SessionLapDomainItem = SessionLapDomainItem(
    segmentId = segmentId,
    lapNumber = lapNumber,
    lapLabel = lapNumber.toString(),
    sessionTypeId = "race",
    sessionTypeLabel = "Race",
    totalTimeMs = totalTimeMs,
    totalTime = totalTime,
    s1Ms = 30_000,
    s1 = "30.000",
    s2Ms = 34_000,
    s2 = "34.000",
    s3Ms = 35_000,
    s3 = "35.000",
    incidentsCount = 0,
    incidents = "0",
    deltaMs = 0,
    delta = "-0.000",
    deltaIsPositive = false,
    status = SessionLapDomainStatus.Clean,
)
