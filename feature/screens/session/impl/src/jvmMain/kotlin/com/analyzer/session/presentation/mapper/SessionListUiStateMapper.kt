package com.analyzer.session.presentation.mapper

import com.analyzer.session.domain.model.SessionFilterOption
import com.analyzer.session.domain.model.SessionListDataset
import com.analyzer.session.domain.model.SessionListDomainItem
import com.analyzer.session.domain.model.SessionListProjection
import com.analyzer.session.domain.model.SessionListQuery
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionFilterKind
import com.analyzer.session.presentation.model.SessionFilterOptionUi
import com.analyzer.session.presentation.model.SessionFilterUiModel
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.analyzer.session.presentation.model.SessionStatsUi
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.Locale

@Inject
@SingleIn(ScreenScope::class)
class SessionListUiStateMapper {

    fun map(
        dataset: SessionListDataset,
        projection: SessionListProjection,
        query: SessionListQuery,
        isLoading: Boolean = false,
        errorOverride: String? = null,
    ): SessionListState = SessionListState(
        isLoading = isLoading,
        error = errorOverride ?: projection.error,
        stats = SessionStatsUi(
            totalDistanceLabel = String.format(Locale.US, "%.3f", dataset.stats.totalDistanceKm),
            sessionsCount = dataset.stats.sessionsCount,
            incidentsCount = dataset.stats.incidentsCount,
            favoriteCar = dataset.stats.favoriteCar,
        ),
        gameFilter = buildFilter(
            kind = SessionFilterKind.Game,
            selectedId = query.gameId,
            options = dataset.gameOptions,
        ),
        trackFilter = buildFilter(
            kind = SessionFilterKind.Track,
            selectedId = query.trackId,
            options = dataset.trackOptions,
        ),
        carFilter = buildFilter(
            kind = SessionFilterKind.Car,
            selectedId = query.carId,
            options = dataset.carOptions,
        ),
        dateFilter = buildFilter(
            kind = SessionFilterKind.Date,
            selectedId = query.dateId,
            options = dataset.dateOptions,
        ),
        sortFilter = buildFilter(
            kind = SessionFilterKind.Sort,
            selectedId = query.sortId,
            options = dataset.sortOptions,
        ),
        searchQuery = query.searchQuery,
        page = projection.page,
        pageCount = projection.pageCount,
        sessions = projection.rows.map { it.toUi() },
        visibleSessions = projection.visibleRows.map { it.toUi() },
    )

    private fun buildFilter(
        kind: SessionFilterKind,
        selectedId: String,
        options: List<SessionFilterOption>,
    ): SessionFilterUiModel {
        val uiOptions = if (options.isEmpty()) {
            listOf(SessionFilterOptionUi(id = FILTER_ALL_ID))
        } else {
            options.map { SessionFilterOptionUi(id = it.id, label = it.label) }
        }
        val resolved = if (uiOptions.any { it.id == selectedId }) selectedId else uiOptions.first().id

        return SessionFilterUiModel(
            kind = kind,
            selectedId = resolved,
            options = uiOptions,
        )
    }

    private fun SessionListDomainItem.toUi(): SessionRowUi = SessionRowUi(
        sessionId = sessionId,
        dateLabel = dateLabel,
        timeLabel = timeLabel,
        gameLabel = gameLabel,
        sessionTypeLabel = sessionTypeLabel,
        trackLabel = trackLabel,
        carLabel = carLabel,
        lapsLabel = lapsLabel,
        bestLapLabel = bestLapLabel,
        isSaved = isSaved,
    )
}
