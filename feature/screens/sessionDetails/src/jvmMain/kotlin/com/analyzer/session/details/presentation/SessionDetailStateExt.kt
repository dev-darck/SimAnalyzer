package com.analyzer.session.details.presentation

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
import com.analyzer.session.details.domain.model.SessionDetailPage
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private val SESSION_DETAIL_SORT_OPTIONS = persistentListOf(
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
)

private val SESSION_DETAIL_SHOW_OPTIONS = persistentListOf(
    SessionDetailFilterOptionUi(id = SESSION_DETAIL_SHOW_ALL),
    SessionDetailFilterOptionUi(id = SESSION_DETAIL_SHOW_VALID),
    SessionDetailFilterOptionUi(id = SESSION_DETAIL_SHOW_INVALID),
    SessionDetailFilterOptionUi(id = SESSION_DETAIL_SHOW_PIT),
)

internal fun SessionDetailPage.toSessionDetailState(
    query: SessionDetailQuery,
    isLoading: Boolean = false,
): SessionDetailState = SessionDetailState(
    isLoading = isLoading,
    error = error,
    header = SessionDetailHeaderUi(
        subtitle = header.subtitle,
        sessionTypeLabel = header.sessionTypeLabel,
        airTempLabel = header.airTempLabel,
        trackTempLabel = header.trackTempLabel,
        carLabel = header.carLabel,
        trackLabel = header.trackLabel,
        savedCarId = header.savedCarId,
        thumbnailPath = header.thumbnailPath,
    ),
    stats = SessionDetailStatsUi(
        bestLapLabel = stats.bestLapLabel,
        averageLapLabel = stats.averageLapLabel,
        incidentsCount = stats.incidentsCount,
    ),
    sortFilter = sessionDetailFilterUiModel(
        kind = SessionDetailFilterKind.Sort,
        selectedId = query.sortId,
        options = SESSION_DETAIL_SORT_OPTIONS,
    ),
    showFilter = sessionDetailFilterUiModel(
        kind = SessionDetailFilterKind.Show,
        selectedId = query.showId,
        options = SESSION_DETAIL_SHOW_OPTIONS,
    ),
    sessionTypeFilter = sessionDetailFilterUiModel(
        kind = SessionDetailFilterKind.SessionType,
        selectedId = query.sessionTypeId,
        options = sessionDetailSessionTypeOptions(),
    ),
    page = page,
    pageCount = pageCount,
    visibleLaps = laps.map(SessionLapDomainItem::toSessionLapRowUi).toImmutableList(),
)

private fun SessionDetailPage.sessionDetailSessionTypeOptions(): ImmutableList<SessionDetailFilterOptionUi> = buildList {
    add(SessionDetailFilterOptionUi(id = SESSION_DETAIL_TYPE_ALL))
    sessionTypeOptions.forEach { option ->
        add(SessionDetailFilterOptionUi(id = option.id, label = option.label))
    }
}.toImmutableList()

private fun sessionDetailFilterUiModel(
    kind: SessionDetailFilterKind,
    selectedId: String,
    options: ImmutableList<SessionDetailFilterOptionUi>,
): SessionDetailFilterUiModel {
    val resolved = if (options.any { it.id == selectedId }) selectedId else options.first().id
    return SessionDetailFilterUiModel(
        kind = kind,
        selectedId = resolved,
        options = options,
    )
}

private fun SessionLapDomainItem.toSessionLapRowUi(): SessionLapRowUi = SessionLapRowUi(
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
    status = status.toLapStatus(),
)

private fun SessionLapDomainStatus.toLapStatus(): LapStatus = when (this) {
    SessionLapDomainStatus.Clean -> LapStatus.Clean
    SessionLapDomainStatus.OutLap -> LapStatus.OutLap
    SessionLapDomainStatus.Dirty -> LapStatus.Dirty
    SessionLapDomainStatus.BestLap -> LapStatus.BestLap
    SessionLapDomainStatus.Invalid -> LapStatus.Invalid
    SessionLapDomainStatus.PitIn -> LapStatus.PitIn
}
