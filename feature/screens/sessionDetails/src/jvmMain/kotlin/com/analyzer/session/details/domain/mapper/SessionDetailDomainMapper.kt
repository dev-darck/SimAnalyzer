package com.analyzer.session.details.domain.mapper

import com.analyzer.session.data.model.RecordedSessionDetailPage
import com.analyzer.session.details.domain.model.SESSION_DETAIL_TYPE_ALL
import com.analyzer.session.details.domain.model.SessionDetailDomainHeader
import com.analyzer.session.details.domain.model.SessionDetailDomainStats
import com.analyzer.session.details.domain.model.SessionDetailPage
import com.analyzer.session.details.domain.model.SessionDetailSessionTypeOption
import com.analyzer.session.details.domain.model.SessionLapDomainItem
import com.analyzer.session.details.domain.model.SessionLapDomainStatus
import com.analyzer.session.details.domain.usecase.SavedCarThumbnailMatch
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.utils.TelemetryIdentityFormatter
import com.project.analyzer.utils.ext.formatDeltaTime
import com.project.analyzer.utils.ext.fromMsToLapTime
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Inject
@SingleIn(ScreenScope::class)
class SessionDetailDomainMapper(
    @param:Default
    private val default: CoroutineDispatcher,
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val zoneId = ZoneId.systemDefault()

    suspend fun map(details: RecordedSessionDetailPage, thumbnail: SavedCarThumbnailMatch? = null): SessionDetailPage =
        withContext(default) {
            val sessionTypeLabel = details.summary.sessionType.toSessionTypeLabel()
            val lapRows = buildLapRows(details.laps, details.stats.bestLapTimeMs)
            val sessionTypeOptions = details.sessionTypeOptions.map { option ->
                SessionDetailSessionTypeOption(
                    id = option.id,
                    label = option.label ?: option.id.toSessionTypeLabel(),
                )
            }
            val defaultSessionTypeId = details.defaultSessionTypeId ?: SESSION_DETAIL_TYPE_ALL

            SessionDetailPage(
                header = buildHeader(
                    details = details,
                    sessionTypeLabel = sessionTypeLabel,
                    thumbnail = thumbnail,
                ),
                stats = SessionDetailDomainStats(
                    bestLapLabel = details.stats.bestLapTimeMs?.fromMsToLapTime() ?: "0:00.000",
                    averageLapLabel = details.stats.averageLapTimeMs?.fromMsToLapTime() ?: "0:00.000",
                    incidentsCount = details.stats.incidentsCount,
                ),
                laps = lapRows,
                sessionTypeOptions = sessionTypeOptions,
                defaultSessionTypeId = defaultSessionTypeId,
                page = details.page,
                pageCount = details.pageCount,
                error = details.error,
            )
        }

    private fun buildHeader(
        details: RecordedSessionDetailPage,
        sessionTypeLabel: String,
        thumbnail: SavedCarThumbnailMatch?,
    ): SessionDetailDomainHeader {
        val summary = details.summary
        val dateLabel = dateFormatter.format(Instant.ofEpochMilli(summary.startedAtMs).atZone(zoneId))
        val timeLabel = timeFormatter.format(Instant.ofEpochMilli(summary.startedAtMs).atZone(zoneId))
        val trackLabel = summary.trackName.toDisplayTrackLabel(summary.trackId)
        val carLabel = summary.carName.toDisplayCarLabel(summary.carModel)
        val airTemp = summary.airTempC.formatTemperatureLabel()
        val trackTemp = summary.trackTempC.formatTemperatureLabel()

        return SessionDetailDomainHeader(
            subtitle = "$dateLabel, $timeLabel",
            sessionTypeLabel = sessionTypeLabel,
            airTempLabel = airTemp,
            trackTempLabel = trackTemp,
            carLabel = carLabel,
            trackLabel = trackLabel,
            savedCarId = thumbnail?.savedCarId,
            thumbnailPath = thumbnail?.texturePath,
        )
    }

    private fun buildLapRows(
        laps: List<com.analyzer.session.data.model.LapSummary>,
        bestLapMs: Int?,
    ): List<SessionLapDomainItem> {
        val firstLap = laps.minByOrNull { it.lap }?.lap
        return laps.map { lap ->
            val totalTimeMs = lap.totalTimeMs
            val isBest = totalTimeMs != null && bestLapMs != null && totalTimeMs == bestLapMs
            val status = resolveStatus(lap, isBest, lap.lap == firstLap)
            val deltaMs = if (totalTimeMs != null && bestLapMs != null) totalTimeMs - bestLapMs else null
            val deltaLabel = if (deltaMs != null) formatDeltaTime(deltaMs, deltaMs >= 0) else "--"
            val sectorTimes = lap.sectorTimesMs
            val s1Ms = sectorTimes.getOrNull(0)
            val s2Ms = sectorTimes.getOrNull(1)
            val s3Ms = sectorTimes.getOrNull(2)
            val incidentsCount = if (lap.invalid) 1 else 0
            val lapSessionTypeId = lap.sessionType.toSessionTypeId()
            val lapSessionTypeLabel = lap.sessionType.toSessionTypeLabel()

            SessionLapDomainItem(
                segmentId = lap.segmentId,
                lapNumber = lap.lap,
                lapLabel = lap.lap.toString(),
                sessionTypeId = lapSessionTypeId,
                sessionTypeLabel = lapSessionTypeLabel,
                totalTimeMs = totalTimeMs,
                totalTime = totalTimeMs?.fromMsToLapTime() ?: "--.--",
                s1Ms = s1Ms,
                s1 = s1Ms.formatSectorMs(),
                s2Ms = s2Ms,
                s2 = s2Ms.formatSectorMs(),
                s3Ms = s3Ms,
                s3 = s3Ms.formatSectorMs(),
                incidentsCount = incidentsCount,
                incidents = incidentsCount.toString(),
                deltaMs = deltaMs,
                delta = deltaLabel,
                deltaIsPositive = deltaMs?.let { it >= 0 } ?: true,
                status = status,
            )
        }
    }

    private fun resolveStatus(
        lap: com.analyzer.session.data.model.LapSummary,
        isBest: Boolean,
        isFirst: Boolean,
    ): SessionLapDomainStatus {
        if (!lap.complete) {
            return if (isFirst) SessionLapDomainStatus.OutLap else SessionLapDomainStatus.Invalid
        }
        if (lap.inPit) return SessionLapDomainStatus.PitIn
        if (lap.invalid) return SessionLapDomainStatus.Dirty
        if (isBest) return SessionLapDomainStatus.BestLap
        return SessionLapDomainStatus.Clean
    }

    private fun String?.toDisplayTrackLabel(trackId: String?): String = this?.takeIf { it.isNotBlank() }
        ?: TelemetryIdentityFormatter.formatTrackName(trackName = null, trackId = trackId)
        ?: "Unknown"

    private fun String?.toDisplayCarLabel(carModel: String?): String = this?.takeIf { it.isNotBlank() }
        ?: TelemetryIdentityFormatter.formatCarName(carModel = carModel)
        ?: "Unknown"

    private fun Int?.formatSectorMs(): String {
        if (this == null) return "--.--"
        return String.format(Locale.US, "%.3f", this / 1000.0)
    }

    private fun Float?.formatTemperatureLabel(): String {
        if (this == null || !this.isFinite()) return "--°C"
        return "${String.format(Locale.US, "%.0f", this)}°C"
    }

    private fun String?.toSessionTypeLabel(): String {
        val raw = this?.trim().orEmpty()
        if (raw.isBlank()) return "Unknown"
        return raw
            .lowercase(Locale.US)
            .split('_')
            .joinToString(" ") { part -> part.replaceFirstChar { c -> c.titlecase(Locale.US) } }
    }

    private fun String?.toSessionTypeId(): String = this
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.lowercase(Locale.US)
        ?: "unknown"
}
