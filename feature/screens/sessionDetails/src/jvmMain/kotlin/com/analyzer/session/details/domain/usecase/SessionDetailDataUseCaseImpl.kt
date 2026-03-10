package com.analyzer.session.details.domain.usecase

import com.analyzer.session.data.repository.RecordedSessionDetailRequest
import com.analyzer.session.data.repository.RecordedSessionLapShow
import com.analyzer.session.data.repository.RecordedSessionLapSort
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.details.domain.mapper.SessionDetailDomainMapper
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SHOW_ALL
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SHOW_INVALID
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SHOW_PIT
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SHOW_VALID
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_BEST
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_DELTA
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_DELTA_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_INCIDENTS
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_INCIDENTS_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_LAP
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_LAP_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S1
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S1_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S2
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S2_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S3
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_S3_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_STATUS
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_STATUS_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_SORT_TOTAL_DESC
import com.analyzer.session.details.domain.model.SESSION_DETAIL_TYPE_ALL
import com.analyzer.session.details.domain.model.SESSION_DETAIL_TYPE_AUTO
import com.analyzer.session.details.domain.model.SessionDetailPageResult
import com.analyzer.session.details.domain.model.SessionDetailQuery
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class SessionDetailDataUseCaseImpl(
    private val repository: RecordedSessionRepository,
    private val domainMapper: SessionDetailDomainMapper,
    private val thumbnailResolver: AceSavedCarThumbnailResolver,
) : SessionDetailDataUseCase {

    override suspend fun loadPage(
        sessionId: Long,
        query: SessionDetailQuery,
        forceRefresh: Boolean,
    ): SessionDetailPageResult? {
        val normalizedQuery = query.normalize()
        val details = repository.loadSessionDetailPage(
            sessionId = sessionId,
            request = normalizedQuery.toRequest(),
            forceRefresh = forceRefresh,
        ) ?: return null
        val thumbnail = thumbnailResolver.resolve(
            gameId = details.summary.gameId,
            carModel = details.summary.carModel,
            sessionStartedAtMs = details.summary.startedAtMs,
        )
        return SessionDetailPageResult(
            query = normalizedQuery.resolved(
                page = details.page,
                selectedSessionTypeId = details.selectedSessionTypeId,
            ),
            page = domainMapper.map(details, thumbnail),
        )
    }
}

private fun SessionDetailQuery.normalize(): SessionDetailQuery = copy(page = page.coerceAtLeast(1))

private fun SessionDetailQuery.toRequest(): RecordedSessionDetailRequest = RecordedSessionDetailRequest(
    sort = sortId.toRecordedLapSort(),
    show = showId.toRecordedLapShow(),
    sessionTypeId = when (sessionTypeId) {
        SESSION_DETAIL_TYPE_AUTO, SESSION_DETAIL_TYPE_ALL -> null
        else -> sessionTypeId
    },
    autoSelectSessionType = sessionTypeId == SESSION_DETAIL_TYPE_AUTO,
    page = page,
)

private fun SessionDetailQuery.resolved(page: Int, selectedSessionTypeId: String?): SessionDetailQuery = copy(
    sessionTypeId = selectedSessionTypeId ?: SESSION_DETAIL_TYPE_ALL,
    page = page,
)

private fun String.toRecordedLapSort(): RecordedSessionLapSort = when (this) {
    SESSION_DETAIL_SORT_LAP_DESC -> RecordedSessionLapSort.LapDesc
    SESSION_DETAIL_SORT_BEST -> RecordedSessionLapSort.TotalTimeAsc
    SESSION_DETAIL_SORT_TOTAL_DESC -> RecordedSessionLapSort.TotalTimeDesc
    SESSION_DETAIL_SORT_S1 -> RecordedSessionLapSort.Sector1Asc
    SESSION_DETAIL_SORT_S1_DESC -> RecordedSessionLapSort.Sector1Desc
    SESSION_DETAIL_SORT_S2 -> RecordedSessionLapSort.Sector2Asc
    SESSION_DETAIL_SORT_S2_DESC -> RecordedSessionLapSort.Sector2Desc
    SESSION_DETAIL_SORT_S3 -> RecordedSessionLapSort.Sector3Asc
    SESSION_DETAIL_SORT_S3_DESC -> RecordedSessionLapSort.Sector3Desc
    SESSION_DETAIL_SORT_INCIDENTS -> RecordedSessionLapSort.IncidentsAsc
    SESSION_DETAIL_SORT_INCIDENTS_DESC -> RecordedSessionLapSort.IncidentsDesc
    SESSION_DETAIL_SORT_DELTA -> RecordedSessionLapSort.DeltaAsc
    SESSION_DETAIL_SORT_DELTA_DESC -> RecordedSessionLapSort.DeltaDesc
    SESSION_DETAIL_SORT_STATUS -> RecordedSessionLapSort.StatusAsc
    SESSION_DETAIL_SORT_STATUS_DESC -> RecordedSessionLapSort.StatusDesc
    SESSION_DETAIL_SORT_LAP -> RecordedSessionLapSort.LapAsc
    else -> RecordedSessionLapSort.LapAsc
}

private fun String.toRecordedLapShow(): RecordedSessionLapShow = when (this) {
    SESSION_DETAIL_SHOW_VALID -> RecordedSessionLapShow.Valid
    SESSION_DETAIL_SHOW_INVALID -> RecordedSessionLapShow.Invalid
    SESSION_DETAIL_SHOW_PIT -> RecordedSessionLapShow.Pit
    SESSION_DETAIL_SHOW_ALL -> RecordedSessionLapShow.All
    else -> RecordedSessionLapShow.All
}
