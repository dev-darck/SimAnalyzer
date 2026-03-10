package com.analyzer.session.domain.usecase

import com.analyzer.session.data.repository.RecordedSessionListRequest
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.data.repository.RecordedSessionSummarySort
import com.analyzer.session.domain.mapper.SessionListDomainMapper
import com.analyzer.session.domain.model.SESSION_LIST_SORT_BEST
import com.analyzer.session.domain.model.SESSION_LIST_SORT_BEST_DESC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_CAR_ASC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_CAR_DESC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_GAME_ASC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_GAME_DESC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_LAPS_ASC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_LAPS_DESC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_NEWEST
import com.analyzer.session.domain.model.SESSION_LIST_SORT_OLDEST
import com.analyzer.session.domain.model.SESSION_LIST_SORT_TRACK_ASC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_TRACK_DESC
import com.analyzer.session.domain.model.SessionListPageResult
import com.analyzer.session.domain.model.SessionListQuery
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class SessionListDataUseCaseImpl(
    private val repository: RecordedSessionRepository,
    private val domainMapper: SessionListDomainMapper,
) : SessionListDataUseCase {

    override suspend fun loadPage(query: SessionListQuery, forceRefresh: Boolean): SessionListPageResult {
        val normalizedQuery = query.normalize()
        val page = repository.loadSessionListPage(
            request = normalizedQuery.toRequest(),
            forceRefresh = forceRefresh,
        )
        return SessionListPageResult(
            query = normalizedQuery.copy(page = page.page),
            page = domainMapper.map(page),
        )
    }

    override suspend fun saveSession(sessionId: Long): Boolean = repository.saveSession(sessionId)

    override suspend fun deleteSession(sessionId: Long): Boolean = repository.deleteSession(sessionId)
}

private fun SessionListQuery.normalize(): SessionListQuery = copy(
    gameId = gameId.normalizeFilterId(),
    trackId = trackId.normalizeFilterId(),
    carId = carId.normalizeFilterId(),
    dateId = dateId.normalizeFilterId(),
    page = page.coerceAtLeast(1),
)

private fun SessionListQuery.toRequest(): RecordedSessionListRequest = RecordedSessionListRequest(
    gameId = gameId.toNullableFilterId(),
    trackId = trackId.toNullableFilterId(),
    carId = carId.toNullableFilterId(),
    dateId = dateId.toNullableFilterId(),
    searchQuery = searchQuery,
    sort = sortId.toRecordedSort(),
    page = page,
)

private fun String.normalizeFilterId(): String = trim().ifBlank { ALL_FILTER_ID }

private fun String.toNullableFilterId(): String? = normalizeFilterId().takeUnless { it == ALL_FILTER_ID }

private fun String.toRecordedSort(): RecordedSessionSummarySort = when (this) {
    SESSION_LIST_SORT_OLDEST -> RecordedSessionSummarySort.StartedAtAsc
    SESSION_LIST_SORT_GAME_ASC -> RecordedSessionSummarySort.GameAsc
    SESSION_LIST_SORT_GAME_DESC -> RecordedSessionSummarySort.GameDesc
    SESSION_LIST_SORT_TRACK_ASC -> RecordedSessionSummarySort.TrackAsc
    SESSION_LIST_SORT_TRACK_DESC -> RecordedSessionSummarySort.TrackDesc
    SESSION_LIST_SORT_CAR_ASC -> RecordedSessionSummarySort.CarAsc
    SESSION_LIST_SORT_CAR_DESC -> RecordedSessionSummarySort.CarDesc
    SESSION_LIST_SORT_LAPS_ASC -> RecordedSessionSummarySort.LapCountAsc
    SESSION_LIST_SORT_LAPS_DESC -> RecordedSessionSummarySort.LapCountDesc
    SESSION_LIST_SORT_BEST -> RecordedSessionSummarySort.BestLapAsc
    SESSION_LIST_SORT_BEST_DESC -> RecordedSessionSummarySort.BestLapDesc
    SESSION_LIST_SORT_NEWEST -> RecordedSessionSummarySort.StartedAtDesc
    else -> RecordedSessionSummarySort.StartedAtDesc
}

private const val ALL_FILTER_ID = "all"
