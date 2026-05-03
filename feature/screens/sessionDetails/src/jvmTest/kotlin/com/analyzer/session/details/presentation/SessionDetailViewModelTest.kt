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
import com.analyzer.session.details.domain.usecase.SessionDetailDataUseCase
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import kotlinx.coroutines.Dispatchers
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
            val viewModel = SessionDetailViewModel(FakeSessionDetailDataUseCase())

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
            val viewModel = SessionDetailViewModel(FakeSessionDetailDataUseCase())

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
}

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
                trackLabel = "Monza",
                sessionTypeLabel = "Race",
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
