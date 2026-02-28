package com.analyzer.session.details.presentation.mapper

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
import com.analyzer.session.details.domain.model.SessionDetailDataset
import com.analyzer.session.details.domain.model.SessionDetailProjection
import com.analyzer.session.details.domain.model.SessionDetailQuery
import com.analyzer.session.details.domain.model.SessionLapDomainItem
import com.analyzer.session.details.domain.model.SessionLapDomainStatus
import com.analyzer.session.details.presentation.model.LapStatus
import com.analyzer.session.details.presentation.model.SessionDetailFilterKind
import com.analyzer.session.details.presentation.model.SessionDetailFilterOptionUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterUiModel
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.analyzer.session.details.presentation.model.SessionLapRowUi
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class SessionDetailUiStateMapper {

    fun map(
        dataset: SessionDetailDataset,
        projection: SessionDetailProjection,
        query: SessionDetailQuery,
        isLoading: Boolean = false,
        errorOverride: String? = null,
    ): SessionDetailState = SessionDetailState(
        isLoading = isLoading,
        error = errorOverride ?: projection.error,
        header = SessionDetailHeaderUi(
            subtitle = dataset.header.subtitle,
            sessionTypeLabel = dataset.header.sessionTypeLabel,
            airTempLabel = dataset.header.airTempLabel,
            trackTempLabel = dataset.header.trackTempLabel,
            carLabel = dataset.header.carLabel,
            trackLabel = dataset.header.trackLabel,
            savedCarId = dataset.header.savedCarId,
            thumbnailPath = dataset.header.thumbnailPath,
        ),
        stats = SessionDetailStatsUi(
            bestLapLabel = dataset.stats.bestLapLabel,
            averageLapLabel = dataset.stats.averageLapLabel,
            incidentsCount = dataset.stats.incidentsCount,
        ),
        sortFilter = buildFilter(
            kind = SessionDetailFilterKind.Sort,
            selectedId = query.sortId,
            options = listOf(
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_LAP),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_LAP_DESC),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_BEST),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_TOTAL_DESC),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_S1),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_S1_DESC),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_S2),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_S2_DESC),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_S3),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_S3_DESC),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_INCIDENTS),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_INCIDENTS_DESC),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_DELTA),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_DELTA_DESC),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_STATUS),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SORT_STATUS_DESC),
            ),
        ),
        showFilter = buildFilter(
            kind = SessionDetailFilterKind.Show,
            selectedId = query.showId,
            options = listOf(
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SHOW_ALL),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SHOW_VALID),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SHOW_INVALID),
                SessionDetailFilterOptionUi(id = SESSION_DETAIL_SHOW_PIT),
            ),
        ),
        sessionTypeFilter = buildFilter(
            kind = SessionDetailFilterKind.SessionType,
            selectedId = query.sessionTypeId,
            options = buildList {
                add(SessionDetailFilterOptionUi(id = SESSION_DETAIL_TYPE_ALL))
                dataset.sessionTypeOptions.forEach { option ->
                    add(SessionDetailFilterOptionUi(id = option.id, label = option.label))
                }
            },
        ),
        page = projection.page,
        pageCount = projection.pageCount,
        laps = projection.laps.map { it.toUi() },
        visibleLaps = projection.visibleLaps.map { it.toUi() },
    )

    private fun buildFilter(
        kind: SessionDetailFilterKind,
        selectedId: String,
        options: List<SessionDetailFilterOptionUi>,
    ): SessionDetailFilterUiModel {
        val resolved = if (options.any { it.id == selectedId }) selectedId else options.first().id
        return SessionDetailFilterUiModel(
            kind = kind,
            selectedId = resolved,
            options = options,
        )
    }

    private fun SessionLapDomainItem.toUi(): SessionLapRowUi = SessionLapRowUi(
        lapNumber = lapNumber,
        lapLabel = lapLabel,
        sessionTypeLabel = sessionTypeLabel,
        totalTimeMs = totalTimeMs,
        totalTime = totalTime,
        s1 = s1,
        s2 = s2,
        s3 = s3,
        incidents = incidents,
        delta = delta,
        deltaIsPositive = deltaIsPositive,
        status = status.toUi(),
    )

    private fun SessionLapDomainStatus.toUi(): LapStatus = when (this) {
        SessionLapDomainStatus.Clean -> LapStatus.Clean
        SessionLapDomainStatus.OutLap -> LapStatus.OutLap
        SessionLapDomainStatus.Dirty -> LapStatus.Dirty
        SessionLapDomainStatus.BestLap -> LapStatus.BestLap
        SessionLapDomainStatus.Invalid -> LapStatus.Invalid
        SessionLapDomainStatus.PitIn -> LapStatus.PitIn
    }
}
