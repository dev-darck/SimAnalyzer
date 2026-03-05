package com.analyzer.session.domain.usecase

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
import com.analyzer.session.domain.model.SESSION_LIST_SORT_TYPE_ASC
import com.analyzer.session.domain.model.SESSION_LIST_SORT_TYPE_DESC
import com.analyzer.session.domain.model.SessionListDataset
import com.analyzer.session.domain.model.SessionListDomainItem
import com.analyzer.session.domain.model.SessionListProjection
import com.analyzer.session.domain.model.SessionListQuery
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.ceil

@Inject
@SingleIn(ScreenScope::class)
class SessionListProjectionUseCaseImpl(
    @param:Default
    private val default: CoroutineDispatcher,
) : SessionListProjectionUseCase {

    override suspend fun project(dataset: SessionListDataset, query: SessionListQuery): SessionListProjection =
        withContext(default) {
            val searchQuery = query.searchQuery.trim().lowercase(Locale.US)
            val filtered = dataset.items.filter { item ->
                val gameMatch = query.gameId == ALL_ID || query.gameId == item.gameId
                val trackMatch = query.trackId == ALL_ID || query.trackId == item.trackId
                val carMatch = query.carId == ALL_ID || query.carId == item.carId
                val dateMatch = query.dateId == ALL_ID || query.dateId == item.dateLabel
                val searchMatch = searchQuery.isBlank() || item.searchText.contains(searchQuery)
                gameMatch && trackMatch && carMatch && dateMatch && searchMatch
            }

            val sorted = when (query.sortId) {
                SESSION_LIST_SORT_NEWEST -> filtered.sortedByDescending(SessionListDomainItem::startedAtMs)

                SESSION_LIST_SORT_OLDEST -> filtered.sortedBy(SessionListDomainItem::startedAtMs)

                SESSION_LIST_SORT_GAME_ASC -> filtered.sortByLabel(SessionListDomainItem::gameLabel, ascending = true)

                SESSION_LIST_SORT_GAME_DESC -> filtered.sortByLabel(SessionListDomainItem::gameLabel, ascending = false)

                SESSION_LIST_SORT_TYPE_ASC -> filtered.sortByLabel(
                    SessionListDomainItem::sessionTypeLabel,
                    ascending = true,
                )

                SESSION_LIST_SORT_TYPE_DESC -> filtered.sortByLabel(
                    SessionListDomainItem::sessionTypeLabel,
                    ascending = false,
                )

                SESSION_LIST_SORT_TRACK_ASC -> filtered.sortByLabel(SessionListDomainItem::trackLabel, ascending = true)

                SESSION_LIST_SORT_TRACK_DESC -> filtered.sortByLabel(
                    SessionListDomainItem::trackLabel,
                    ascending = false,
                )

                SESSION_LIST_SORT_CAR_ASC -> filtered.sortByLabel(SessionListDomainItem::carLabel, ascending = true)

                SESSION_LIST_SORT_CAR_DESC -> filtered.sortByLabel(SessionListDomainItem::carLabel, ascending = false)

                SESSION_LIST_SORT_LAPS_ASC -> filtered.sortByNumeric(SessionListDomainItem::lapCount, ascending = true)

                SESSION_LIST_SORT_LAPS_DESC -> filtered.sortByNumeric(
                    SessionListDomainItem::lapCount,
                    ascending = false,
                )

                SESSION_LIST_SORT_BEST_DESC -> filtered.sortByBestLap(ascending = false)

                SESSION_LIST_SORT_BEST -> filtered.sortByBestLap(ascending = true)

                else -> filtered.sortByBestLap(ascending = true)
            }

            val pageCount = maxOf(1, ceil(sorted.size / PAGE_SIZE.toDouble()).toInt())
            val page = query.page.coerceIn(1, pageCount)
            val visibleRows = sorted.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE)

            SessionListProjection(
                rows = sorted,
                visibleRows = visibleRows,
                page = page,
                pageCount = pageCount,
                error = if (sorted.isEmpty() && dataset.items.isNotEmpty()) "No sessions match filters." else null,
            )
        }

    private companion object {

        const val ALL_ID = "all"
        const val PAGE_SIZE = 8
    }
}

private fun List<SessionListDomainItem>.sortByLabel(
    selector: (SessionListDomainItem) -> String,
    ascending: Boolean,
): List<SessionListDomainItem> {
    val comparator = if (ascending) {
        compareBy<SessionListDomainItem> { selector(it).lowercase(Locale.US) }
            .thenByDescending { it.startedAtMs }
    } else {
        compareByDescending<SessionListDomainItem> { selector(it).lowercase(Locale.US) }
            .thenByDescending { it.startedAtMs }
    }
    return sortedWith(comparator)
}

private fun List<SessionListDomainItem>.sortByNumeric(
    selector: (SessionListDomainItem) -> Int,
    ascending: Boolean,
): List<SessionListDomainItem> {
    val comparator = if (ascending) {
        compareBy<SessionListDomainItem>(selector).thenByDescending { it.startedAtMs }
    } else {
        compareByDescending<SessionListDomainItem>(selector).thenByDescending { it.startedAtMs }
    }
    return sortedWith(comparator)
}

private fun List<SessionListDomainItem>.sortByBestLap(ascending: Boolean): List<SessionListDomainItem> {
    val comparator = if (ascending) {
        compareBy<SessionListDomainItem> { it.bestLapTimeMs ?: Int.MAX_VALUE }
            .thenByDescending { it.startedAtMs }
    } else {
        compareBy<SessionListDomainItem> { it.bestLapTimeMs == null }
            .thenByDescending { it.bestLapTimeMs ?: Int.MIN_VALUE }
            .thenByDescending { it.startedAtMs }
    }
    return sortedWith(comparator)
}
