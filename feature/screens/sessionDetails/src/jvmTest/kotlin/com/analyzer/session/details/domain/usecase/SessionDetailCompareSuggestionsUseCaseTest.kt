package com.analyzer.session.details.domain.usecase

import com.analyzer.session.data.model.RecordedSessionDetailPage
import com.analyzer.session.data.model.RecordedSessionListPage
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.repository.RecordedSessionDetailRequest
import com.analyzer.session.data.repository.RecordedSessionListRequest
import com.analyzer.session.data.repository.RecordedSessionRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionDetailCompareSuggestionsUseCaseTest {

    @Test
    fun `use case keeps only same layout sessions and ranks same car first`() = kotlinx.coroutines.test.runTest {
        val repository = FakeRecordedSessionRepository(
            sessions = listOf(
                session(sessionId = 42L, carId = 296, layoutId = "gp", startedAtMs = 5L),
                session(sessionId = 77L, carId = 296, layoutId = "gp", startedAtMs = 4L),
                session(sessionId = 81L, carId = 300, layoutId = "gp", startedAtMs = 6L),
                session(sessionId = 82L, carId = 300, layoutId = "short", startedAtMs = 7L),
                session(sessionId = 83L, carId = 300, layoutId = null, startedAtMs = 8L),
            ),
        )
        val useCase = SessionDetailCompareSuggestionsUseCaseImpl(repository)

        val result = useCase.loadSuggestions(
            criteria = SessionDetailCompareCriteria(
                currentSessionId = 42L,
                gameId = "acc",
                gameLabel = "ACC",
                trackId = "monza",
                layoutId = "gp",
                trackLabel = "Monza GP",
                preferredCarIdentityKey = "296",
            ),
        )

        assertEquals("acc", repository.requests.single().gameId)
        assertEquals("monza", repository.requests.single().trackId)
        assertEquals(listOf(77L, 81L), result.candidates.map { it.sessionId })
        assertEquals("Same car", result.candidates.first().recommendationLabel)
        assertEquals(2, result.excludedDifferentLayoutCount)
    }

    @Test
    fun `use case allows sessions without layout when both sides have none`() = kotlinx.coroutines.test.runTest {
        val repository = FakeRecordedSessionRepository(
            sessions = listOf(
                session(sessionId = 11L, layoutId = null, carId = 201, startedAtMs = 1L),
                session(sessionId = 12L, layoutId = null, carId = 202, startedAtMs = 2L),
            ),
        )
        val useCase = SessionDetailCompareSuggestionsUseCaseImpl(repository)

        val result = useCase.loadSuggestions(
            criteria = SessionDetailCompareCriteria(
                currentSessionId = 11L,
                gameId = "acc",
                gameLabel = "ACC",
                trackId = "monza",
                layoutId = null,
                trackLabel = "Monza",
                preferredCarIdentityKey = null,
            ),
        )

        assertEquals(listOf(12L), result.candidates.map { it.sessionId })
        assertTrue(result.excludedDifferentLayoutCount == 0)
    }
}

private class FakeRecordedSessionRepository(
    private val sessions: List<RecordedSessionSummary>,
) : RecordedSessionRepository {

    val requests = mutableListOf<RecordedSessionListRequest>()

    override suspend fun loadSessionListPage(
        request: RecordedSessionListRequest,
        forceRefresh: Boolean,
    ): RecordedSessionListPage {
        requests += request
        val filtered = sessions.filter { summary ->
            (request.gameId == null || request.gameId == summary.gameId) &&
                (request.trackId == null || request.trackId == summary.trackId)
        }
        return RecordedSessionListPage(
            items = filtered,
            page = request.page,
            pageCount = 1,
        )
    }

    override suspend fun loadSessionDetailPage(
        sessionId: Long,
        request: RecordedSessionDetailRequest,
        forceRefresh: Boolean,
    ): RecordedSessionDetailPage? = null

    override suspend fun saveSession(sessionId: Long): Boolean = false

    override suspend fun deleteSession(sessionId: Long): Boolean = false
}

private fun session(
    sessionId: Long,
    carId: Int?,
    layoutId: String?,
    startedAtMs: Long,
): RecordedSessionSummary = RecordedSessionSummary(
    sessionId = sessionId,
    startedAtMs = startedAtMs,
    endedAtMs = null,
    gameId = "acc",
    sessionType = "race",
    carModel = "bmw_m4_gt3",
    carName = "BMW M4 GT3",
    carId = carId,
    trackId = "monza",
    trackName = "Monza",
    layoutId = layoutId,
    lapCount = 10,
    bestLapTimeMs = 98_100,
    totalIncidents = 0,
    distanceKm = 100.0,
    isSaved = true,
)
