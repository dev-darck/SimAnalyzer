package com.analyzer.session.presentation

import com.analyzer.session.domain.model.SessionFilterOption
import com.analyzer.session.domain.model.SessionListDomainItem
import com.analyzer.session.domain.model.SessionListPage
import com.analyzer.session.domain.model.SessionListQuery
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionFilterKind
import com.analyzer.session.presentation.model.SessionFilterOptionUi
import com.analyzer.session.presentation.model.SessionFilterUiModel
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.analyzer.session.presentation.model.SessionStatsUi
import java.util.Locale

internal fun SessionListPage.toSessionListState(
    query: SessionListQuery,
    isLoading: Boolean = false,
): SessionListState = SessionListState(
    isLoading = isLoading,
    error = error,
    stats = SessionStatsUi(
        totalDistanceLabel = String.format(Locale.US, "%.3f", stats.totalDistanceKm),
        sessionsCount = stats.sessionsCount,
        incidentsCount = stats.incidentsCount,
        favoriteCar = stats.favoriteCar,
    ),
    gameFilter = gameOptions.toSessionFilterUiModel(
        kind = SessionFilterKind.Game,
        selectedId = query.gameId,
    ),
    trackFilter = trackOptions.toSessionFilterUiModel(
        kind = SessionFilterKind.Track,
        selectedId = query.trackId,
    ),
    carFilter = carOptions.toSessionFilterUiModel(
        kind = SessionFilterKind.Car,
        selectedId = query.carId,
    ),
    dateFilter = dateOptions.toSessionFilterUiModel(
        kind = SessionFilterKind.Date,
        selectedId = query.dateId,
    ),
    sortFilter = sortOptions.toSessionFilterUiModel(
        kind = SessionFilterKind.Sort,
        selectedId = query.sortId,
    ),
    searchQuery = query.searchQuery,
    page = page,
    pageCount = pageCount,
    visibleSessions = rows.map(SessionListDomainItem::toSessionRowUi),
)

private fun List<SessionFilterOption>.toSessionFilterUiModel(
    kind: SessionFilterKind,
    selectedId: String,
): SessionFilterUiModel {
    val uiOptions = if (isEmpty()) {
        listOf(SessionFilterOptionUi(id = FILTER_ALL_ID))
    } else {
        map { option -> SessionFilterOptionUi(id = option.id, label = option.label) }
    }
    val resolved = if (uiOptions.any { it.id == selectedId }) selectedId else uiOptions.first().id

    return SessionFilterUiModel(
        kind = kind,
        selectedId = resolved,
        options = uiOptions,
    )
}

private fun SessionListDomainItem.toSessionRowUi(): SessionRowUi = SessionRowUi(
    sessionId = sessionId,
    dateLabel = dateLabel,
    timeLabel = timeLabel,
    gameId = gameId,
    trackId = trackId,
    layoutId = layoutId,
    gameLabel = gameLabel,
    sessionTypeLabel = sessionTypeLabel,
    trackLabel = trackLabel,
    carLabel = carLabel,
    lapsLabel = lapsLabel,
    bestLapLabel = bestLapLabel,
    isSaved = isSaved,
    trackMap = null,
)
