package com.analyzer.session.domain.mapper

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
import com.analyzer.session.domain.model.SessionListDataset
import com.analyzer.session.domain.model.SessionListDomainItem
import com.analyzer.session.domain.model.SessionListDomainStats
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

    suspend fun map(
        sessions: List<RecordedSessionSummary>,
    ): SessionListDataset = withContext(default) {
        val items = sessions.map(::mapItem)
        SessionListDataset(
            items = items,
            stats = mapStats(sessions),
            gameOptions = buildGameOptions(items),
            trackOptions = buildIdentityOptions(items.map { it.trackId to it.trackLabel }),
            carOptions = buildIdentityOptions(items.map { it.carId to it.carLabel }),
            dateOptions = buildLabelOptions(items.map { it.dateLabel }),
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
        )
    }

    private fun mapItem(
        session: RecordedSessionSummary,
    ): SessionListDomainItem {
        val gameId = normalizeGameId(session.gameId)
        val gameLabel = gameLabel(gameId)
        val sessionTypeLabel = session.sessionType.toSessionTypeLabel()
        val trackId = session.trackId?.takeIf { it.isNotBlank() }
        val carId = session.carId?.takeIf { it > 0 }?.toString()
            ?: session.carModel?.takeIf { it.isNotBlank() }
        val trackLabel = session.trackName.toDisplayTrackLabel(trackId)
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

    private fun mapStats(sessions: List<RecordedSessionSummary>): SessionListDomainStats {
        val favoriteCar = sessions
            .mapNotNull { summary ->
                summary.carName.toDisplayCarLabel(summary.carModel).takeIf { it.isNotBlank() }
            }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: "-"

        return SessionListDomainStats(
            totalDistanceKm = sessions.sumOf { it.distanceKm },
            sessionsCount = sessions.size,
            incidentsCount = sessions.sumOf { it.totalIncidents },
            favoriteCar = favoriteCar,
        )
    }

    private fun buildGameOptions(items: List<SessionListDomainItem>): List<SessionFilterOption> = listOf(
        SessionFilterOption(id = "all"),
    ) + items
        .asSequence()
        .map { SessionFilterOption(id = it.gameId, label = it.gameLabel) }
        .distinctBy { it.id }
        .sortedBy { it.label }
        .toList()

    private fun buildLabelOptions(values: List<String>): List<SessionFilterOption> = listOf(
        SessionFilterOption(id = "all"),
    ) + values
        .asSequence()
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
        .map { SessionFilterOption(id = it, label = it) }
        .toList()

    private fun buildIdentityOptions(values: List<Pair<String, String>>): List<SessionFilterOption> = listOf(
        SessionFilterOption(id = "all"),
    ) + values
        .asSequence()
        .filter { (id, label) -> id.isNotBlank() && label.isNotBlank() }
        .distinctBy { it.first }
        .sortedBy { it.second }
        .map { (id, label) -> SessionFilterOption(id = id, label = label) }
        .toList()

    private fun formatDate(epochMs: Long): String = dateFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun formatTime(epochMs: Long): String = timeFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun String?.toDisplayTrackLabel(trackId: String?): String = this?.takeIf { it.isNotBlank() }
        ?: TelemetryIdentityFormatter.formatTrackName(trackName = null, trackId = trackId)
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
