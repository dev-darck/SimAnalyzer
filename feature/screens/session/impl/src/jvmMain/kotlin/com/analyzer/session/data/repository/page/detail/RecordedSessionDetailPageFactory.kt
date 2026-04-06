package com.analyzer.session.data.repository.page.detail

import com.analyzer.session.data.analysis.IndexAnalysis
import com.analyzer.session.data.model.LapSummary
import com.analyzer.session.data.model.RecordedSessionDetailPage
import com.analyzer.session.data.model.RecordedSessionDetailStats
import com.analyzer.session.data.model.RecordedSessionOption
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.model.SessionBundleLocation
import com.analyzer.session.data.model.SessionLocation
import com.analyzer.session.data.repository.RecordedSessionDetailRequest
import com.analyzer.session.data.repository.RecordedSessionLapShow
import com.analyzer.session.data.repository.RecordedSessionLapSort
import com.analyzer.session.data.repository.shouldSkipPlaceholderLocation
import dev.zacsweers.metro.Inject
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil

@Inject
internal class RecordedSessionDetailPageFactory {

    private val datasetCache = AtomicReference<SessionDetailDatasetCache?>()
    private val projectionCache = AtomicReference<SessionDetailProjectionCache?>()

    fun buildPage(
        bundle: SessionBundleLocation,
        request: RecordedSessionDetailRequest,
        analysisLoader: (SessionLocation) -> IndexAnalysis?,
    ): RecordedSessionDetailPage {
        val dataset = resolveDataset(bundle, analysisLoader)
        val defaultSessionTypeId = resolveDefaultSessionTypeId(
            summary = dataset.summary,
            sessionTypeOptions = dataset.sessionTypeOptions,
        )
        val selectedSessionTypeId = resolveSelectedSessionTypeId(
            request = request,
            sessionTypeOptions = dataset.sessionTypeOptions,
            defaultSessionTypeId = defaultSessionTypeId,
        )
        val projection = resolveProjection(
            dataset = dataset,
            request = request,
            selectedSessionTypeId = selectedSessionTypeId,
        )
        val pageSize = request.pageSize.coerceAtLeast(1)
        val pageCount = maxOf(1, ceil(projection.laps.size / pageSize.toDouble()).toInt())
        val page = request.page.coerceIn(1, pageCount)

        return RecordedSessionDetailPage(
            summary = dataset.summary,
            stats = dataset.stats,
            sessionTypeOptions = dataset.sessionTypeOptions,
            defaultSessionTypeId = defaultSessionTypeId,
            selectedSessionTypeId = selectedSessionTypeId,
            laps = projection.laps.drop((page - 1) * pageSize).take(pageSize),
            page = page,
            pageCount = pageCount,
            error = projection.error,
        )
    }

    private fun resolveDataset(
        bundle: SessionBundleLocation,
        analysisLoader: (SessionLocation) -> IndexAnalysis?,
    ): SessionDetailDataset {
        datasetCache.get()
            ?.takeIf { cache -> cache.source == bundle }
            ?.data
            ?.let { return it }

        val laps = collectBundleLaps(bundle, analysisLoader)
        val data = SessionDetailDataset(
            source = bundle,
            summary = bundle.summary,
            laps = laps,
            stats = buildSessionDetailStats(laps),
            sessionTypeOptions = buildSessionTypeOptions(laps),
            firstLapNumber = laps.firstLapNumber(),
        )
        datasetCache.set(SessionDetailDatasetCache(source = bundle, data = data))
        if (projectionCache.get()?.source !== bundle) {
            projectionCache.set(null)
        }
        return data
    }

    private fun resolveProjection(
        dataset: SessionDetailDataset,
        request: RecordedSessionDetailRequest,
        selectedSessionTypeId: String?,
    ): SessionDetailProjectionData {
        val key = SessionDetailProjectionKey(
            sort = request.sort,
            show = request.show,
            selectedSessionTypeId = selectedSessionTypeId,
        )

        projectionCache.get()
            ?.takeIf { cache -> cache.source == dataset.source && cache.key == key }
            ?.data
            ?.let { return it }

        val filtered = dataset.laps
            .filter { lap -> selectedSessionTypeId == null || lap.sessionTypeId() == selectedSessionTypeId }
            .filter { lap -> lap.matches(request.show, dataset.stats.bestLapTimeMs, dataset.firstLapNumber) }

        val data = SessionDetailProjectionData(
            laps = filtered.sortedWith(
                lapSummaryComparator(
                    sort = request.sort,
                    bestLapTimeMs = dataset.stats.bestLapTimeMs,
                    firstLapNumber = dataset.firstLapNumber,
                ),
            ),
            error = if (filtered.isEmpty() && dataset.laps.isNotEmpty()) "No laps match filters." else null,
        )
        projectionCache.set(
            SessionDetailProjectionCache(
                source = dataset.source,
                key = key,
                data = data,
            ),
        )
        return data
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

    private fun RecordedSessionSummary.sessionTypeId(): String? = sessionType
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.lowercase(Locale.US)

    private fun LapSummary.sessionTypeId(): String = sessionType
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.lowercase(Locale.US)
        ?: "unknown"

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
}
