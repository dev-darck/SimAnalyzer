package com.analyzer.session.data.repository.page.list

import com.analyzer.session.data.model.RecordedSessionListPage
import com.analyzer.session.data.model.RecordedSessionListStats
import com.analyzer.session.data.model.RecordedSessionOption
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.repository.RecordedSessionListRequest
import com.analyzer.session.data.repository.RecordedSessionSummarySort
import com.project.analyzer.utils.TelemetryIdentityFormatter
import dev.zacsweers.metro.Inject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil

@Inject
internal class RecordedSessionListPageFactory {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val zoneId = ZoneId.systemDefault()

    private val baseCache = AtomicReference<SessionListBaseCache?>()
    private val projectionCache = AtomicReference<SessionListProjectionCache?>()

    fun buildPage(
        summaries: List<RecordedSessionSummary>,
        request: RecordedSessionListRequest,
    ): RecordedSessionListPage {
        val baseData = resolveBaseData(summaries)
        val projection = resolveProjection(summaries, request)
        val pageSize = request.pageSize.coerceAtLeast(1)
        val pageCount = maxOf(1, ceil(projection.items.size / pageSize.toDouble()).toInt())
        val page = request.page.coerceIn(1, pageCount)

        return RecordedSessionListPage(
            stats = baseData.stats,
            gameOptions = baseData.gameOptions,
            trackOptions = baseData.trackOptions,
            carOptions = baseData.carOptions,
            dateOptions = baseData.dateOptions,
            items = projection.items.drop((page - 1) * pageSize).take(pageSize),
            page = page,
            pageCount = pageCount,
            error = projection.error,
        )
    }

    private fun resolveBaseData(summaries: List<RecordedSessionSummary>): SessionListBaseData {
        baseCache.get()
            ?.takeIf { cache -> cache.source === summaries }
            ?.data
            ?.let { return it }

        val data = SessionListBaseData(
            stats = buildSessionListStats(summaries),
            gameOptions = buildGameOptions(summaries),
            trackOptions = buildTrackOptions(summaries),
            carOptions = buildCarOptions(summaries),
            dateOptions = buildDateOptions(summaries),
        )
        baseCache.set(SessionListBaseCache(source = summaries, data = data))
        if (projectionCache.get()?.source !== summaries) {
            projectionCache.set(null)
        }
        return data
    }

    private fun resolveProjection(
        summaries: List<RecordedSessionSummary>,
        request: RecordedSessionListRequest,
    ): SessionListProjectionData {
        val key = SessionListProjectionKey(
            gameId = request.gameId,
            trackId = request.trackId,
            carId = request.carId,
            dateId = request.dateId,
            searchQuery = request.searchQuery.trim().lowercase(Locale.US),
            sort = request.sort,
        )

        projectionCache.get()
            ?.takeIf { cache -> cache.source === summaries && cache.key == key }
            ?.data
            ?.let { return it }

        val filtered = summaries.filter { summary -> summary.matches(request, key.searchQuery) }
        val data = SessionListProjectionData(
            items = filtered.sortedWith(sessionSummaryComparator(request.sort)),
            error = if (filtered.isEmpty() && summaries.isNotEmpty()) "No sessions match filters." else null,
        )
        projectionCache.set(
            SessionListProjectionCache(
                source = summaries,
                key = key,
                data = data,
            ),
        )
        return data
    }

    private fun buildSessionListStats(summaries: List<RecordedSessionSummary>): RecordedSessionListStats {
        val favoriteCar = summaries
            .asSequence()
            .mapNotNull { summary ->
                summary.carIdentityKey()?.let { key -> key to summary }
            }
            .groupingBy { it.first }
            .aggregate { _, accumulator: FavoriteCarAccumulator?, element, first ->
                val summary = element.second
                if (first || accumulator == null) {
                    FavoriteCarAccumulator(
                        count = 1,
                        carModel = summary.carModel,
                        carName = summary.carName,
                    )
                } else {
                    accumulator.copy(count = accumulator.count + 1)
                }
            }
            .maxByOrNull { it.value.count }
            ?.value

        return RecordedSessionListStats(
            totalDistanceKm = summaries.sumOf(RecordedSessionSummary::distanceKm),
            sessionsCount = summaries.size,
            incidentsCount = summaries.sumOf(RecordedSessionSummary::totalIncidents),
            favoriteCarModel = favoriteCar?.carModel,
            favoriteCarName = favoriteCar?.carName,
        )
    }

    private fun buildGameOptions(summaries: List<RecordedSessionSummary>): List<RecordedSessionOption> = summaries
        .asSequence()
        .map { summary ->
            val gameId = normalizeGameId(summary.gameId)
            RecordedSessionOption(
                id = gameId,
                label = gameLabel(gameId),
            )
        }
        .distinctBy(RecordedSessionOption::id)
        .sortedBy { it.label.orEmpty() }
        .toList()

    private fun buildTrackOptions(summaries: List<RecordedSessionSummary>): List<RecordedSessionOption> = summaries
        .asSequence()
        .mapNotNull { summary ->
            summary.trackId?.takeIf(String::isNotBlank)?.let { trackId ->
                RecordedSessionOption(
                    id = trackId,
                    label = summary.trackName.toDisplayTrackLabel(
                        trackId = trackId,
                        layoutId = summary.layoutId,
                    ),
                )
            }
        }
        .distinctBy(RecordedSessionOption::id)
        .sortedBy { it.label.orEmpty() }
        .toList()

    private fun buildCarOptions(summaries: List<RecordedSessionSummary>): List<RecordedSessionOption> = summaries
        .asSequence()
        .mapNotNull { summary ->
            summary.carIdentityKey()?.let { carId ->
                RecordedSessionOption(
                    id = carId,
                    label = summary.carName.toDisplayCarLabel(summary.carModel),
                )
            }
        }
        .distinctBy(RecordedSessionOption::id)
        .sortedBy { it.label.orEmpty() }
        .toList()

    private fun buildDateOptions(summaries: List<RecordedSessionSummary>): List<RecordedSessionOption> = summaries
        .asSequence()
        .map { summary -> formatDate(summary.startedAtMs) }
        .distinct()
        .sorted()
        .map { label -> RecordedSessionOption(id = label, label = label) }
        .toList()

    private fun RecordedSessionSummary.matches(
        request: RecordedSessionListRequest,
        normalizedSearchQuery: String,
    ): Boolean {
        if (request.gameId != null && request.gameId != normalizeGameId(gameId)) return false
        if (request.trackId != null && request.trackId != trackId) return false
        if (request.carId != null && request.carId != carIdentityKey()) return false
        if (request.dateId != null && request.dateId != formatDate(startedAtMs)) return false
        if (normalizedSearchQuery.isBlank()) return true
        return searchText().contains(normalizedSearchQuery)
    }

    private fun RecordedSessionSummary.searchText(): String = listOfNotNull(
        normalizeGameId(gameId),
        gameLabel(normalizeGameId(gameId)),
        sessionType.toSessionTypeLabel(),
        trackName.toDisplayTrackLabel(trackId = trackId, layoutId = layoutId),
        carName.toDisplayCarLabel(carModel),
        trackId,
        carIdentityKey(),
    ).joinToString("|").lowercase(Locale.US)

    private fun sessionSummaryComparator(sort: RecordedSessionSummarySort): Comparator<RecordedSessionSummary> =
        when (sort) {
            RecordedSessionSummarySort.StartedAtDesc -> compareByDescending(RecordedSessionSummary::startedAtMs)

            RecordedSessionSummarySort.StartedAtAsc -> compareBy(RecordedSessionSummary::startedAtMs)

            RecordedSessionSummarySort.GameAsc -> compareByLabel(true) { summary ->
                gameLabel(normalizeGameId(summary.gameId))
            }

            RecordedSessionSummarySort.GameDesc -> compareByLabel(false) { summary ->
                gameLabel(normalizeGameId(summary.gameId))
            }

            RecordedSessionSummarySort.TrackAsc -> compareByLabel(true) { summary ->
                summary.trackName.toDisplayTrackLabel(trackId = summary.trackId, layoutId = summary.layoutId)
            }

            RecordedSessionSummarySort.TrackDesc -> compareByLabel(false) { summary ->
                summary.trackName.toDisplayTrackLabel(trackId = summary.trackId, layoutId = summary.layoutId)
            }

            RecordedSessionSummarySort.CarAsc -> compareByLabel(true) { summary ->
                summary.carName.toDisplayCarLabel(summary.carModel)
            }

            RecordedSessionSummarySort.CarDesc -> compareByLabel(false) { summary ->
                summary.carName.toDisplayCarLabel(summary.carModel)
            }

            RecordedSessionSummarySort.LapCountAsc -> compareBy<RecordedSessionSummary> { it.lapCount }
                .thenByDescending(RecordedSessionSummary::startedAtMs)

            RecordedSessionSummarySort.LapCountDesc -> compareByDescending<RecordedSessionSummary> { it.lapCount }
                .thenByDescending(RecordedSessionSummary::startedAtMs)

            RecordedSessionSummarySort.BestLapAsc -> compareBy<RecordedSessionSummary> {
                it.bestLapTimeMs ?: Int.MAX_VALUE
            }.thenByDescending(RecordedSessionSummary::startedAtMs)

            RecordedSessionSummarySort.BestLapDesc -> compareBy<RecordedSessionSummary> { it.bestLapTimeMs == null }
                .thenByDescending { it.bestLapTimeMs ?: Int.MIN_VALUE }
                .thenByDescending(RecordedSessionSummary::startedAtMs)
        }

    private fun <T> compareByLabel(ascending: Boolean, selector: (T) -> String): Comparator<T> = if (ascending) {
        compareBy<T> { item -> selector(item).lowercase(Locale.US) }
    } else {
        compareByDescending<T> { item -> selector(item).lowercase(Locale.US) }
    }

    private fun RecordedSessionSummary.carIdentityKey(): String? = carId
        ?.takeIf { it > 0 }
        ?.toString()
        ?: carModel?.takeIf(String::isNotBlank)

    private fun String?.toDisplayTrackLabel(trackId: String?, layoutId: String?): String =
        this?.takeIf(String::isNotBlank)
            ?: TelemetryIdentityFormatter.formatTrackName(
                trackName = null,
                trackId = trackId,
                layoutId = layoutId,
            )
            ?: "Unknown"

    private fun String?.toDisplayCarLabel(carModel: String?): String = this?.takeIf(String::isNotBlank)
        ?: TelemetryIdentityFormatter.formatCarName(carModel = carModel)
        ?: "Unknown"

    private fun String?.toSessionTypeLabel(): String {
        val raw = this?.trim().orEmpty()
        if (raw.isBlank()) return "Unknown"
        return raw
            .lowercase(Locale.US)
            .split('_')
            .joinToString(" ") { part -> part.replaceFirstChar { c -> c.titlecase(Locale.US) } }
    }

    private fun formatDate(epochMs: Long): String = dateFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun normalizeGameId(gameId: String?): String = gameId.orEmpty().trim().lowercase()

    private fun gameLabel(gameId: String): String = when (gameId) {
        "ac" -> "AC"
        "ace" -> "AC Evo"
        "acc" -> "ACC"
        "lmu" -> "LMU"
        "unknown" -> "Unknown"
        else -> gameId.uppercase(Locale.US)
    }
}
