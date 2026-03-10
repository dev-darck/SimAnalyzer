package com.analyzer.session.domain.mapper

import com.analyzer.session.data.model.RecordedSessionListPage
import com.analyzer.session.data.model.RecordedSessionListStats
import com.analyzer.session.data.model.RecordedSessionOption
import com.analyzer.session.data.model.RecordedSessionSummary
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
import com.analyzer.session.domain.model.SessionFilterOption
import com.analyzer.session.domain.model.SessionListDomainItem
import com.analyzer.session.domain.model.SessionListDomainStats
import com.analyzer.session.domain.model.SessionListPage
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.utils.TelemetryIdentityFormatter
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
class SessionListDomainMapper(
    @param:Default
    private val default: CoroutineDispatcher,
) {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val zoneId = ZoneId.systemDefault()

    suspend fun map(page: RecordedSessionListPage): SessionListPage = withContext(default) {
        SessionListPage(
            stats = mapStats(page.stats),
            gameOptions = page.gameOptions.toFilterOptions(),
            trackOptions = page.trackOptions.toFilterOptions(),
            carOptions = page.carOptions.toFilterOptions(),
            dateOptions = page.dateOptions.toFilterOptions(),
            sortOptions = listOf(
                SessionFilterOption(SESSION_LIST_SORT_NEWEST),
                SessionFilterOption(SESSION_LIST_SORT_OLDEST),
                SessionFilterOption(SESSION_LIST_SORT_GAME_ASC),
                SessionFilterOption(SESSION_LIST_SORT_GAME_DESC),
                SessionFilterOption(SESSION_LIST_SORT_TRACK_ASC),
                SessionFilterOption(SESSION_LIST_SORT_TRACK_DESC),
                SessionFilterOption(SESSION_LIST_SORT_CAR_ASC),
                SessionFilterOption(SESSION_LIST_SORT_CAR_DESC),
                SessionFilterOption(SESSION_LIST_SORT_LAPS_ASC),
                SessionFilterOption(SESSION_LIST_SORT_LAPS_DESC),
                SessionFilterOption(SESSION_LIST_SORT_BEST),
                SessionFilterOption(SESSION_LIST_SORT_BEST_DESC),
            ),
            rows = page.items.map(::mapItem),
            page = page.page,
            pageCount = page.pageCount,
            error = page.error,
        )
    }

    private fun mapItem(session: RecordedSessionSummary): SessionListDomainItem {
        val gameId = normalizeGameId(session.gameId)
        val gameLabel = gameLabel(gameId)
        val sessionTypeLabel = session.sessionType.toSessionTypeLabel()
        val trackId = session.trackId?.takeIf { it.isNotBlank() }
        val layoutId = session.layoutId?.takeIf { it.isNotBlank() }
        val carId = session.carId?.takeIf { it > 0 }?.toString()
            ?: session.carModel?.takeIf { it.isNotBlank() }
        val trackLabel = session.trackName.toDisplayTrackLabel(trackId, layoutId)
        val carLabel = session.carName.toDisplayCarLabel(session.carModel)
        val dateLabel = formatDate(session.startedAtMs)
        val timeLabel = formatTime(session.startedAtMs)
        val bestLapLabel = session.bestLapTimeMs?.fromMsToLapTime() ?: "0:00.000"
        val searchText = listOf(gameId, gameLabel, sessionTypeLabel, trackLabel, carLabel, trackId, carId)
            .joinToString(separator = "|")
            .lowercase(Locale.US)

        return SessionListDomainItem(
            sessionId = session.sessionId,
            startedAtMs = session.startedAtMs,
            bestLapTimeMs = session.bestLapTimeMs,
            lapCount = session.lapCount,
            gameId = gameId,
            gameLabel = gameLabel,
            sessionTypeLabel = sessionTypeLabel,
            trackId = trackId ?: trackLabel,
            layoutId = layoutId,
            trackLabel = trackLabel,
            carId = carId ?: carLabel,
            carLabel = carLabel,
            dateLabel = dateLabel,
            timeLabel = timeLabel,
            lapsLabel = session.lapCount.toString(),
            bestLapLabel = bestLapLabel,
            isSaved = session.isSaved,
            searchText = searchText,
        )
    }

    private fun mapStats(stats: RecordedSessionListStats): SessionListDomainStats {
        val favoriteCar = stats.favoriteCarName
            .toDisplayCarLabel(stats.favoriteCarModel)
            .takeIf { it.isNotBlank() }
            ?: "-"
        return SessionListDomainStats(
            totalDistanceKm = stats.totalDistanceKm,
            sessionsCount = stats.sessionsCount,
            incidentsCount = stats.incidentsCount,
            favoriteCar = favoriteCar,
        )
    }

    private fun List<RecordedSessionOption>.toFilterOptions(): List<SessionFilterOption> = listOf(
        SessionFilterOption(id = "all"),
    ) + asSequence()
        .filter { option -> option.id.isNotBlank() }
        .map { option -> SessionFilterOption(id = option.id, label = option.label) }
        .sortedBy { it.label }
        .toList()

    private fun formatDate(epochMs: Long): String = dateFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun formatTime(epochMs: Long): String = timeFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun String?.toDisplayTrackLabel(trackId: String?, layoutId: String?): String =
        this?.takeIf { it.isNotBlank() }
            ?: TelemetryIdentityFormatter.formatTrackName(trackName = null, trackId = trackId, layoutId = layoutId)
            ?: "Unknown"

    private fun String?.toDisplayCarLabel(carModel: String?): String = this?.takeIf { it.isNotBlank() }
        ?: TelemetryIdentityFormatter.formatCarName(carModel = carModel)
        ?: "Unknown"

    private fun normalizeGameId(gameId: String): String = gameId.trim().lowercase(Locale.US).ifBlank { "unknown" }

    private fun gameLabel(gameId: String): String = when (val normalized = normalizeGameId(gameId)) {
        "ac" -> "AC"
        "ace" -> "AC Evo"
        "acc" -> "ACC"
        "lmu" -> "LMU"
        "unknown" -> "Unknown"
        else -> normalized.uppercase(Locale.US)
    }

    private fun String?.toSessionTypeLabel(): String {
        val raw = this?.trim().orEmpty()
        if (raw.isBlank()) return "Unknown"
        return raw
            .lowercase(Locale.US)
            .split('_')
            .joinToString(" ") { part -> part.replaceFirstChar { c -> c.titlecase(Locale.US) } }
    }
}
