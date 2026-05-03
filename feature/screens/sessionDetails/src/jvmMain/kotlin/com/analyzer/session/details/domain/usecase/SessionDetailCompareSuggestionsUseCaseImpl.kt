package com.analyzer.session.details.domain.usecase

import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.repository.RecordedSessionListRequest
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.data.repository.RecordedSessionSummarySort
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.utils.TelemetryIdentityFormatter
import com.project.analyzer.utils.ext.fromMsToLapTime
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Inject
@SingleIn(ScreenScope::class)
internal class SessionDetailCompareSuggestionsUseCaseImpl(private val repository: RecordedSessionRepository,) :
    SessionDetailCompareSuggestionsUseCase {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val zoneId = ZoneId.systemDefault()

    override suspend fun loadSuggestions(
        criteria: SessionDetailCompareCriteria,
        forceRefresh: Boolean,
    ): SessionDetailCompareSuggestions {
        val matches = mutableListOf<RecordedSessionSummary>()
        var excludedDifferentLayoutCount = 0
        var page = 1
        var pageCount = 1

        while (page <= pageCount && matches.size < CANDIDATE_LIMIT) {
            val result = repository.loadSessionListPage(
                request = RecordedSessionListRequest(
                    gameId = criteria.gameId,
                    trackId = criteria.trackId,
                    sort = RecordedSessionSummarySort.StartedAtDesc,
                    page = page,
                    pageSize = PAGE_SIZE,
                ),
                forceRefresh = forceRefresh,
            )
            pageCount = result.pageCount
            result.items.forEach { session ->
                when {
                    session.sessionId == criteria.currentSessionId -> Unit
                    !matchesLayout(criteria.layoutId, session.layoutId) -> excludedDifferentLayoutCount += 1
                    else -> matches += session
                }
            }
            if (result.items.isEmpty()) break
            page += 1
        }

        val candidates = matches
            .sortedWith(
                compareByDescending<RecordedSessionSummary> { criteria.preferredCarIdentityKey == it.carIdentityKey() }
                    .thenByDescending { it.startedAtMs },
            )
            .take(CANDIDATE_LIMIT)
            .map { session ->
                val sameCar = criteria.preferredCarIdentityKey != null &&
                    criteria.preferredCarIdentityKey == session.carIdentityKey()
                SessionDetailCompareSuggestion(
                    sessionId = session.sessionId,
                    carLabel = session.carName.toDisplayCarLabel(session.carModel),
                    sessionTypeLabel = session.sessionType.toSessionTypeLabel(),
                    dateLabel = formatDate(session.startedAtMs),
                    timeLabel = formatTime(session.startedAtMs),
                    bestLapLabel = session.bestLapTimeMs?.fromMsToLapTime() ?: "0:00.000",
                    lapsLabel = session.lapCount.toString(),
                    recommendationLabel = if (sameCar) "Same car" else "Same track",
                )
            }

        return SessionDetailCompareSuggestions(
            candidates = candidates,
            excludedDifferentLayoutCount = excludedDifferentLayoutCount,
        )
    }

    private fun matchesLayout(expectedLayoutId: String?, actualLayoutId: String?): Boolean {
        val normalizedExpected = expectedLayoutId.normalizeIdentity()
        val normalizedActual = actualLayoutId.normalizeIdentity()
        return when {
            normalizedExpected == null && normalizedActual == null -> true
            normalizedExpected == null || normalizedActual == null -> false
            else -> normalizedExpected == normalizedActual
        }
    }

    private fun formatDate(epochMs: Long): String = dateFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun formatTime(epochMs: Long): String = timeFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun RecordedSessionSummary.carIdentityKey(): String? = carId
        ?.takeIf { it > 0 }
        ?.toString()
        ?: carModel.normalizeIdentity()

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

    private fun String?.normalizeIdentity(): String? = this
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.lowercase(Locale.US)

    private companion object {
        const val PAGE_SIZE = 32
        const val CANDIDATE_LIMIT = 12
    }
}
