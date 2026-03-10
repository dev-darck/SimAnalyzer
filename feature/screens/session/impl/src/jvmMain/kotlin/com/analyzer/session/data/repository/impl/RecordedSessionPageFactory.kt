package com.analyzer.session.data.repository.impl

import com.analyzer.session.data.model.LapSummary
import com.analyzer.session.data.model.RecordedSessionDetailPage
import com.analyzer.session.data.model.RecordedSessionDetailStats
import com.analyzer.session.data.model.RecordedSessionListPage
import com.analyzer.session.data.model.RecordedSessionListStats
import com.analyzer.session.data.model.RecordedSessionOption
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.repository.RecordedSessionDetailRequest
import com.analyzer.session.data.repository.RecordedSessionLapShow
import com.analyzer.session.data.repository.RecordedSessionLapSort
import com.analyzer.session.data.repository.RecordedSessionListRequest
import com.analyzer.session.data.repository.RecordedSessionSummarySort
import com.project.analyzer.utils.TelemetryIdentityFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

internal class RecordedSessionPageFactory {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val zoneId = ZoneId.systemDefault()

    fun buildSessionListPage(
        summaries: List<RecordedSessionSummary>,
        request: RecordedSessionListRequest,
    ): RecordedSessionListPage {
        val filtered = summaries.filter { summary -> summary.matches(request) }
        val sorted = filtered.sortedWith(sessionSummaryComparator(request.sort))
        val pageSize = request.pageSize.coerceAtLeast(1)
        val pageCount = maxOf(1, ceil(sorted.size / pageSize.toDouble()).toInt())
        val page = request.page.coerceIn(1, pageCount)

        return RecordedSessionListPage(
            stats = buildSessionListStats(summaries),
            gameOptions = buildGameOptions(summaries),
            trackOptions = buildTrackOptions(summaries),
            carOptions = buildCarOptions(summaries),
            dateOptions = buildDateOptions(summaries),
            items = sorted.drop((page - 1) * pageSize).take(pageSize),
            page = page,
            pageCount = pageCount,
            error = if (sorted.isEmpty() && summaries.isNotEmpty()) "No sessions match filters." else null,
        )
    }

    fun buildSessionDetailPage(
        bundle: SessionBundleLocation,
        request: RecordedSessionDetailRequest,
        analysisLoader: (SessionLocation) -> IndexAnalysis?,
    ): RecordedSessionDetailPage {
        val summary = bundle.summary
        val laps = collectBundleLaps(bundle, analysisLoader)
        val stats = buildSessionDetailStats(laps)
        val sessionTypeOptions = buildSessionTypeOptions(laps)
        val defaultSessionTypeId = resolveDefaultSessionTypeId(
            summary = summary,
            sessionTypeOptions = sessionTypeOptions,
        )
        val selectedSessionTypeId = resolveSelectedSessionTypeId(
            request = request,
            sessionTypeOptions = sessionTypeOptions,
            defaultSessionTypeId = defaultSessionTypeId,
        )
        val firstLapNumber = laps.firstLapNumber()
        val filtered = laps
            .filter { lap -> selectedSessionTypeId == null || lap.sessionTypeId() == selectedSessionTypeId }
            .filter { lap -> lap.matches(request.show, stats.bestLapTimeMs, firstLapNumber) }
        val sorted = filtered.sortedWith(
            lapSummaryComparator(
                sort = request.sort,
                bestLapTimeMs = stats.bestLapTimeMs,
                firstLapNumber = firstLapNumber,
            ),
        )
        val pageSize = request.pageSize.coerceAtLeast(1)
        val pageCount = maxOf(1, ceil(sorted.size / pageSize.toDouble()).toInt())
        val page = request.page.coerceIn(1, pageCount)

        return RecordedSessionDetailPage(
            summary = summary,
            stats = stats,
            sessionTypeOptions = sessionTypeOptions,
            defaultSessionTypeId = defaultSessionTypeId,
            selectedSessionTypeId = selectedSessionTypeId,
            laps = sorted.drop((page - 1) * pageSize).take(pageSize),
            page = page,
            pageCount = pageCount,
            error = if (sorted.isEmpty() && laps.isNotEmpty()) "No laps match filters." else null,
        )
    }

    private fun collectBundleLaps(
        bundle: SessionBundleLocation,
        analysisLoader: (SessionLocation) -> IndexAnalysis?,
    ): List<LapSummary> = bundle.locations.flatMap { location ->
        val analysis = location.analysis ?: analysisLoader(location)
        if (shouldSkipPlaceholderLocation(bundle, location, analysis)) {
            return@flatMap emptyList()
        }
        val sessionType = location.metadata.sessionType
        analysis
            ?.laps
            .orEmpty()
            .map { lap -> lap.copy(sessionType = sessionType) }
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

    private fun buildSessionTypeOptions(laps: List<LapSummary>): List<RecordedSessionOption> = laps
        .asSequence()
        .mapNotNull { lap ->
            lap.sessionTypeId().takeIf(String::isNotBlank)?.let { id ->
                RecordedSessionOption(
                    id = id,
                    label = lap.sessionType.toSessionTypeLabel(),
                )
            }
        }
        .distinctBy(RecordedSessionOption::id)
        .toList()

    private fun resolveDefaultSessionTypeId(
        summary: RecordedSessionSummary,
        sessionTypeOptions: List<RecordedSessionOption>,
    ): String? {
        val availableIds = sessionTypeOptions.mapTo(linkedSetOf(), RecordedSessionOption::id)
        val summaryTypeId = summary.sessionTypeId()
        return when {
            summaryTypeId != null && availableIds.contains(summaryTypeId) -> summaryTypeId
            sessionTypeOptions.isNotEmpty() -> sessionTypeOptions.last().id
            else -> null
        }
    }

    private fun resolveSelectedSessionTypeId(
        request: RecordedSessionDetailRequest,
        sessionTypeOptions: List<RecordedSessionOption>,
        defaultSessionTypeId: String?,
    ): String? {
        val availableIds = sessionTypeOptions.mapTo(linkedSetOf(), RecordedSessionOption::id)
        return when {
            request.autoSelectSessionType -> defaultSessionTypeId
            request.sessionTypeId == null -> null
            availableIds.contains(request.sessionTypeId) -> request.sessionTypeId
            else -> defaultSessionTypeId
        }
    }

    private fun buildSessionDetailStats(laps: List<LapSummary>): RecordedSessionDetailStats {
        val bestLapTimeMs = laps
            .filter { lap -> lap.complete && !lap.invalid && !lap.inPit }
            .mapNotNull(LapSummary::totalTimeMs)
            .minOrNull()
        val averageLapTimeMs = laps
            .filter { lap -> lap.complete && !lap.invalid && !lap.inPit }
            .mapNotNull(LapSummary::totalTimeMs)
            .average()
            .takeIf(Double::isFinite)
            ?.toInt()
        return RecordedSessionDetailStats(
            bestLapTimeMs = bestLapTimeMs,
            averageLapTimeMs = averageLapTimeMs,
            incidentsCount = laps.count(LapSummary::invalid),
        )
    }

    private fun RecordedSessionSummary.matches(request: RecordedSessionListRequest): Boolean {
        if (request.gameId != null && request.gameId != normalizeGameId(gameId)) return false
        if (request.trackId != null && request.trackId != trackId) return false
        if (request.carId != null && request.carId != carIdentityKey()) return false
        if (request.dateId != null && request.dateId != formatDate(startedAtMs)) return false
        val normalizedSearchQuery = request.searchQuery.trim().lowercase(Locale.US)
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

            RecordedSessionSummarySort.GameAsc -> compareByLabel(
                true,
            ) { summary -> gameLabel(normalizeGameId(summary.gameId)) }

            RecordedSessionSummarySort.GameDesc -> compareByLabel(
                false,
            ) { summary -> gameLabel(normalizeGameId(summary.gameId)) }

            RecordedSessionSummarySort.TrackAsc -> compareByLabel(true) { summary ->
                summary.trackName.toDisplayTrackLabel(trackId = summary.trackId, layoutId = summary.layoutId)
            }

            RecordedSessionSummarySort.TrackDesc -> compareByLabel(false) { summary ->
                summary.trackName.toDisplayTrackLabel(trackId = summary.trackId, layoutId = summary.layoutId)
            }

            RecordedSessionSummarySort.CarAsc -> compareByLabel(
                true,
            ) { summary -> summary.carName.toDisplayCarLabel(summary.carModel) }

            RecordedSessionSummarySort.CarDesc -> compareByLabel(
                false,
            ) { summary -> summary.carName.toDisplayCarLabel(summary.carModel) }

            RecordedSessionSummarySort.LapCountAsc -> compareBy<RecordedSessionSummary> { it.lapCount }
                .thenByDescending(RecordedSessionSummary::startedAtMs)

            RecordedSessionSummarySort.LapCountDesc -> compareByDescending<RecordedSessionSummary> { it.lapCount }
                .thenByDescending(RecordedSessionSummary::startedAtMs)

            RecordedSessionSummarySort.BestLapAsc -> compareBy<RecordedSessionSummary> {
                it.bestLapTimeMs
                    ?: Int.MAX_VALUE
            }
                .thenByDescending(RecordedSessionSummary::startedAtMs)

            RecordedSessionSummarySort.BestLapDesc -> compareBy<RecordedSessionSummary> { it.bestLapTimeMs == null }
                .thenByDescending { it.bestLapTimeMs ?: Int.MIN_VALUE }
                .thenByDescending(RecordedSessionSummary::startedAtMs)
        }

    private fun LapSummary.matches(show: RecordedSessionLapShow, bestLapTimeMs: Int?, firstLapNumber: Int?): Boolean =
        when (show) {
            RecordedSessionLapShow.All -> true

            RecordedSessionLapShow.Valid -> lapStatus(bestLapTimeMs, firstLapNumber).let { status ->
                status == RecordedLapStatus.Clean || status == RecordedLapStatus.BestLap
            }

            RecordedSessionLapShow.Invalid -> lapStatus(bestLapTimeMs, firstLapNumber).let { status ->
                status == RecordedLapStatus.Invalid || status == RecordedLapStatus.Dirty
            }

            RecordedSessionLapShow.Pit -> lapStatus(bestLapTimeMs, firstLapNumber) == RecordedLapStatus.PitIn
        }

    private fun lapSummaryComparator(
        sort: RecordedSessionLapSort,
        bestLapTimeMs: Int?,
        firstLapNumber: Int?,
    ): Comparator<LapSummary> = when (sort) {
        RecordedSessionLapSort.LapAsc -> compareBy(LapSummary::lap)

        RecordedSessionLapSort.LapDesc -> compareByDescending<LapSummary> { it.lap }

        RecordedSessionLapSort.TotalTimeAsc -> compareByNullableInt(true) { lap -> lap.totalTimeMs }

        RecordedSessionLapSort.TotalTimeDesc -> compareByNullableInt(false) { lap -> lap.totalTimeMs }

        RecordedSessionLapSort.Sector1Asc -> compareByNullableInt(true) { lap -> lap.sectorTimesMs.getOrNull(0) }

        RecordedSessionLapSort.Sector1Desc -> compareByNullableInt(false) { lap -> lap.sectorTimesMs.getOrNull(0) }

        RecordedSessionLapSort.Sector2Asc -> compareByNullableInt(true) { lap -> lap.sectorTimesMs.getOrNull(1) }

        RecordedSessionLapSort.Sector2Desc -> compareByNullableInt(false) { lap -> lap.sectorTimesMs.getOrNull(1) }

        RecordedSessionLapSort.Sector3Asc -> compareByNullableInt(true) { lap -> lap.sectorTimesMs.getOrNull(2) }

        RecordedSessionLapSort.Sector3Desc -> compareByNullableInt(false) { lap -> lap.sectorTimesMs.getOrNull(2) }

        RecordedSessionLapSort.IncidentsAsc -> compareBy<LapSummary> { it.invalid.incidentCount() }
            .thenBy(LapSummary::lap)

        RecordedSessionLapSort.IncidentsDesc -> compareByDescending<LapSummary> { it.invalid.incidentCount() }
            .thenBy(LapSummary::lap)

        RecordedSessionLapSort.DeltaAsc -> compareByNullableInt(true) { lap -> lap.deltaMs(bestLapTimeMs) }

        RecordedSessionLapSort.DeltaDesc -> compareByNullableInt(false) { lap -> lap.deltaMs(bestLapTimeMs) }

        RecordedSessionLapSort.StatusAsc -> compareBy<LapSummary> {
            it.lapStatus(bestLapTimeMs, firstLapNumber).name
        }.thenBy(LapSummary::lap)

        RecordedSessionLapSort.StatusDesc -> compareByDescending<LapSummary> {
            it.lapStatus(bestLapTimeMs, firstLapNumber).name
        }.thenBy(LapSummary::lap)
    }

    private fun <T> compareByLabel(ascending: Boolean, selector: (T) -> String): Comparator<T> = if (ascending) {
        compareBy<T> { item -> selector(item).lowercase(Locale.US) }
    } else {
        compareByDescending<T> { item -> selector(item).lowercase(Locale.US) }
    }

    private fun compareByNullableInt(ascending: Boolean, selector: (LapSummary) -> Int?): Comparator<LapSummary> =
        if (ascending) {
            compareBy<LapSummary> { lap -> selector(lap) == null }
                .thenBy { lap -> selector(lap) ?: Int.MAX_VALUE }
                .thenBy(LapSummary::lap)
        } else {
            compareBy<LapSummary> { lap -> selector(lap) == null }
                .thenByDescending { lap -> selector(lap) ?: Int.MIN_VALUE }
                .thenBy(LapSummary::lap)
        }

    private fun RecordedSessionSummary.carIdentityKey(): String? = carId
        ?.takeIf { it > 0 }
        ?.toString()
        ?: carModel?.takeIf(String::isNotBlank)

    private fun RecordedSessionSummary.sessionTypeId(): String? = sessionType
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.lowercase(Locale.US)

    private fun LapSummary.sessionTypeId(): String = sessionType
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.lowercase(Locale.US)
        ?: "unknown"

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

    private fun formatDate(epochMs: Long): String = dateFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun gameLabel(gameId: String): String = when (gameId) {
        "ac" -> "AC"
        "ace" -> "AC Evo"
        "acc" -> "ACC"
        "lmu" -> "LMU"
        "unknown" -> "Unknown"
        else -> gameId.uppercase(Locale.US)
    }

    private fun String?.toSessionTypeLabel(): String {
        val raw = this?.trim().orEmpty()
        if (raw.isBlank()) return "Unknown"
        return raw
            .lowercase(Locale.US)
            .split('_')
            .joinToString(" ") { part -> part.replaceFirstChar { c -> c.titlecase(Locale.US) } }
    }

    private fun List<LapSummary>.firstLapNumber(): Int? = minByOrNull(LapSummary::lap)?.lap

    private fun LapSummary.deltaMs(bestLapTimeMs: Int?): Int? {
        val totalTime = totalTimeMs ?: return null
        val bestLapTime = bestLapTimeMs ?: return null
        return totalTime - bestLapTime
    }

    private fun Boolean.incidentCount(): Int = if (this) 1 else 0

    private fun LapSummary.lapStatus(bestLapTimeMs: Int?, firstLapNumber: Int?): RecordedLapStatus {
        val isBest = totalTimeMs != null && bestLapTimeMs != null && totalTimeMs == bestLapTimeMs
        if (!complete) {
            return if (lap == firstLapNumber) RecordedLapStatus.OutLap else RecordedLapStatus.Invalid
        }
        if (inPit) return RecordedLapStatus.PitIn
        if (invalid) return RecordedLapStatus.Dirty
        if (isBest) return RecordedLapStatus.BestLap
        return RecordedLapStatus.Clean
    }

    private fun normalizeGameId(gameId: String?): String = gameId.orEmpty().trim().lowercase()

    private data class FavoriteCarAccumulator(val count: Int, val carModel: String?, val carName: String?)
}

private enum class RecordedLapStatus {
    Clean,
    OutLap,
    Dirty,
    BestLap,
    Invalid,
    PitIn,
}
