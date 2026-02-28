package com.analyzer.session.details.domain.usecase

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
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.math.ceil

@Inject
@SingleIn(ScreenScope::class)
class SessionDetailProjectionUseCase(
    @param:Default
    private val default: CoroutineDispatcher,
) {

    suspend fun project(dataset: SessionDetailDataset, query: SessionDetailQuery): SessionDetailProjection =
        withContext(default) {
            val typeFiltered = when (query.sessionTypeId) {
                SESSION_DETAIL_TYPE_ALL -> dataset.laps
                else -> dataset.laps.filter { it.sessionTypeId == query.sessionTypeId }
            }

            val filtered = when (query.showId) {
                SESSION_DETAIL_SHOW_VALID -> typeFiltered.filter {
                    it.status == SessionLapDomainStatus.Clean || it.status == SessionLapDomainStatus.BestLap
                }

                SESSION_DETAIL_SHOW_INVALID -> typeFiltered.filter {
                    it.status == SessionLapDomainStatus.Invalid || it.status == SessionLapDomainStatus.Dirty
                }

                SESSION_DETAIL_SHOW_PIT -> typeFiltered.filter { it.status == SessionLapDomainStatus.PitIn }

                else -> typeFiltered
            }

            val sorted = when (query.sortId) {
                SESSION_DETAIL_SORT_BEST -> filtered.sortByNullableInt(
                    ascending = true,
                    selector = SessionLapDomainItem::totalTimeMs,
                )

                SESSION_DETAIL_SORT_TOTAL_DESC -> filtered.sortByNullableInt(
                    ascending = false,
                    selector = SessionLapDomainItem::totalTimeMs,
                )

                SESSION_DETAIL_SORT_LAP_DESC -> filtered.sortByInt(
                    ascending = false,
                    selector = SessionLapDomainItem::lapNumber,
                )

                SESSION_DETAIL_SORT_S1 -> filtered.sortByNullableInt(
                    ascending = true,
                    selector = SessionLapDomainItem::s1Ms,
                )

                SESSION_DETAIL_SORT_S1_DESC -> filtered.sortByNullableInt(
                    ascending = false,
                    selector = SessionLapDomainItem::s1Ms,
                )

                SESSION_DETAIL_SORT_S2 -> filtered.sortByNullableInt(
                    ascending = true,
                    selector = SessionLapDomainItem::s2Ms,
                )

                SESSION_DETAIL_SORT_S2_DESC -> filtered.sortByNullableInt(
                    ascending = false,
                    selector = SessionLapDomainItem::s2Ms,
                )

                SESSION_DETAIL_SORT_S3 -> filtered.sortByNullableInt(
                    ascending = true,
                    selector = SessionLapDomainItem::s3Ms,
                )

                SESSION_DETAIL_SORT_S3_DESC -> filtered.sortByNullableInt(
                    ascending = false,
                    selector = SessionLapDomainItem::s3Ms,
                )

                SESSION_DETAIL_SORT_INCIDENTS -> filtered.sortByInt(
                    ascending = true,
                    selector = SessionLapDomainItem::incidentsCount,
                )

                SESSION_DETAIL_SORT_INCIDENTS_DESC -> filtered.sortByInt(
                    ascending = false,
                    selector = SessionLapDomainItem::incidentsCount,
                )

                SESSION_DETAIL_SORT_DELTA -> filtered.sortByNullableInt(
                    ascending = true,
                    selector = SessionLapDomainItem::deltaMs,
                )

                SESSION_DETAIL_SORT_DELTA_DESC -> filtered.sortByNullableInt(
                    ascending = false,
                    selector = SessionLapDomainItem::deltaMs,
                )

                SESSION_DETAIL_SORT_STATUS -> filtered.sortByString(ascending = true) { it.status.name }

                SESSION_DETAIL_SORT_STATUS_DESC -> filtered.sortByString(ascending = false) { it.status.name }

                SESSION_DETAIL_SORT_LAP -> filtered.sortByInt(
                    ascending = true,
                    selector = SessionLapDomainItem::lapNumber
                )

                else -> filtered.sortByInt(ascending = true, selector = SessionLapDomainItem::lapNumber)
            }

            val pageCount = maxOf(1, ceil(sorted.size / PAGE_SIZE.toDouble()).toInt())
            val page = query.page.coerceIn(1, pageCount)
            val visibleLaps = sorted.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE)

            SessionDetailProjection(
                laps = sorted,
                visibleLaps = visibleLaps,
                page = page,
                pageCount = pageCount,
                error = if (sorted.isEmpty() && dataset.laps.isNotEmpty()) "No laps match filters." else null,
            )
        }

    private companion object {

        const val PAGE_SIZE = 10
    }
}

private fun List<SessionLapDomainItem>.sortByInt(
    ascending: Boolean,
    selector: (SessionLapDomainItem) -> Int,
): List<SessionLapDomainItem> {
    val comparator = if (ascending) {
        compareBy<SessionLapDomainItem>(selector).thenBy(SessionLapDomainItem::lapNumber)
    } else {
        compareByDescending<SessionLapDomainItem>(selector).thenBy(SessionLapDomainItem::lapNumber)
    }
    return sortedWith(comparator)
}

private fun List<SessionLapDomainItem>.sortByNullableInt(
    ascending: Boolean,
    selector: (SessionLapDomainItem) -> Int?,
): List<SessionLapDomainItem> {
    val comparator = if (ascending) {
        compareBy<SessionLapDomainItem> { selector(it) == null }
            .thenBy { selector(it) ?: Int.MAX_VALUE }
            .thenBy(SessionLapDomainItem::lapNumber)
    } else {
        compareBy<SessionLapDomainItem> { selector(it) == null }
            .thenByDescending { selector(it) ?: Int.MIN_VALUE }
            .thenBy(SessionLapDomainItem::lapNumber)
    }
    return sortedWith(comparator)
}

private fun List<SessionLapDomainItem>.sortByString(
    ascending: Boolean,
    selector: (SessionLapDomainItem) -> String,
): List<SessionLapDomainItem> {
    val comparator = if (ascending) {
        compareBy<SessionLapDomainItem> { selector(it) }
            .thenBy(SessionLapDomainItem::lapNumber)
    } else {
        compareByDescending<SessionLapDomainItem> { selector(it) }
            .thenBy(SessionLapDomainItem::lapNumber)
    }
    return sortedWith(comparator)
}
