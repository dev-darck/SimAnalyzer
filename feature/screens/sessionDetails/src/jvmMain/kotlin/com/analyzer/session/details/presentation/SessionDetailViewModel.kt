package com.analyzer.session.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.analyzer.session.data.model.LapSummary
import com.analyzer.session.data.model.RecordedSessionDetail
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.details.presentation.model.DropdownFilterUi
import com.analyzer.session.details.presentation.model.DropdownOptionUi
import com.analyzer.session.details.presentation.model.LapStatus
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.analyzer.session.details.presentation.model.SessionLapRowUi
import com.project.analyzer.utils.ext.formatDeltaTime
import com.project.analyzer.utils.ext.fromMsToLapTime
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

@Inject
class SessionDetailViewModel(
    private val repository: RecordedSessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionDetailState())
    val state: StateFlow<SessionDetailState> = _state.asStateFlow()

    private var baseLaps: List<SessionLapRowUi> = emptyList()
    private var currentSessionId: Long? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val zoneId = ZoneId.systemDefault()

    fun setSession(sessionId: Long) {
        if (currentSessionId == sessionId) return
        currentSessionId = sessionId
        load(sessionId)
    }

    fun dispatch(intent: SessionDetailIntent) {
        when (intent) {
            SessionDetailIntent.Refresh -> reload()
            is SessionDetailIntent.ChangeSort -> updateFilter { copy(sortFilter = sortFilter.updateSelection(intent.optionId)) }
            is SessionDetailIntent.ChangeFilter -> updateFilter { copy(showFilter = showFilter.updateSelection(intent.optionId)) }
            is SessionDetailIntent.ChangePage -> updateFilter { copy(page = intent.page) }
        }
    }

    private fun reload() {
        val sessionId = currentSessionId ?: return
        load(sessionId)
    }

    private fun load(sessionId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val details = repository.loadSessionDetails(sessionId)
            if (details == null) {
                baseLaps = emptyList()
                _state.update { it.copy(isLoading = false, error = "Session data not found.") }
                return@launch
            }
            val updated = buildState(details)
            _state.value = applyFilters(updated)
        }
    }

    private fun updateFilter(mutator: SessionDetailState.() -> SessionDetailState) {
        val updated = mutator(_state.value)
        _state.value = applyFilters(updated)
    }

    private fun buildState(details: RecordedSessionDetail): SessionDetailState {
        val bestLapMs = details.laps
            .filter { it.complete && !it.invalid }
            .mapNotNull { it.totalTimeMs }
            .minOrNull()
        val averageLapMs = details.laps
            .filter { it.complete && !it.invalid }
            .mapNotNull { it.totalTimeMs }
            .average()
            .takeIf { it.isFinite() }
            ?.toInt()

        val header = buildHeader(details)
        val stats = SessionDetailStatsUi(
            bestLapLabel = bestLapMs?.fromMsToLapTime() ?: "0:00.000",
            averageLapLabel = averageLapMs?.fromMsToLapTime() ?: "0:00.000",
            incidentsCount = details.laps.count { it.invalid }
        )

        baseLaps = buildLapRows(details.laps, bestLapMs)

        return SessionDetailState(
            isLoading = false,
            error = null,
            header = header,
            stats = stats,
            sortFilter = buildFilter(
                label = "Sort by",
                selectedId = _state.value.sortFilter.selectedId,
                options = listOf(
                    DropdownOptionUi("lap", "Lap"),
                    DropdownOptionUi("best", "Best lap"),
                )
            ),
            showFilter = buildFilter(
                label = "Show",
                selectedId = _state.value.showFilter.selectedId,
                options = listOf(
                    DropdownOptionUi("all", "All laps"),
                    DropdownOptionUi("valid", "Valid laps"),
                    DropdownOptionUi("invalid", "Invalid laps"),
                    DropdownOptionUi("pit", "Pit laps"),
                )
            ),
            page = 1,
            pageCount = 1,
            laps = baseLaps,
            visibleLaps = baseLaps,
        )
    }

    private fun applyFilters(state: SessionDetailState): SessionDetailState {
        val filtered = when (state.showFilter.selectedId) {
            "valid" -> baseLaps.filter { it.status == LapStatus.Clean || it.status == LapStatus.BestLap }
            "invalid" -> baseLaps.filter { it.status == LapStatus.Invalid || it.status == LapStatus.Dirty }
            "pit" -> baseLaps.filter { it.status == LapStatus.PitIn }
            else -> baseLaps
        }

        val sorted = when (state.sortFilter.selectedId) {
            "best" -> filtered.sortedBy { it.totalTimeMs ?: Int.MAX_VALUE }
            else -> filtered.sortedBy { it.lapNumber }
        }

        val pageCount = maxOf(1, ceil(sorted.size / PAGE_SIZE.toDouble()).toInt())
        val page = state.page.coerceIn(1, pageCount)
        val visible = sorted.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE)

        return state.copy(
            isLoading = false,
            error = if (sorted.isEmpty() && baseLaps.isNotEmpty()) "No laps match filters." else null,
            page = page,
            pageCount = pageCount,
            laps = sorted,
            visibleLaps = visible
        )
    }

    private fun buildLapRows(laps: List<LapSummary>, bestLapMs: Int?): List<SessionLapRowUi> {
        val firstLap = laps.minByOrNull { it.lap }?.lap
        return laps.map { lap ->
            val totalTimeMs = lap.totalTimeMs
            val isBest = totalTimeMs != null && bestLapMs != null && totalTimeMs == bestLapMs
            val status = resolveStatus(lap, isBest, lap.lap == firstLap)
            val deltaMs = if (totalTimeMs != null && bestLapMs != null) totalTimeMs - bestLapMs else null
            val deltaLabel = if (deltaMs != null) {
                formatDeltaTime(deltaMs, deltaMs >= 0)
            } else {
                "--"
            }
            val sectorTimes = lap.sectorTimesMs
            val incidents = if (lap.invalid) 1 else 0

            SessionLapRowUi(
                lapNumber = lap.lap,
                lapLabel = lap.lap.toString(),
                totalTimeMs = totalTimeMs,
                totalTime = totalTimeMs?.fromMsToLapTime() ?: "--.--",
                s1 = sectorTimes.getOrNull(0).formatSectorMs(),
                s2 = sectorTimes.getOrNull(1).formatSectorMs(),
                s3 = sectorTimes.getOrNull(2).formatSectorMs(),
                incidents = incidents.toString(),
                delta = deltaLabel,
                deltaIsPositive = deltaMs?.let { it >= 0 } ?: true,
                status = status,
            )
        }
    }

    private fun resolveStatus(lap: LapSummary, isBest: Boolean, isFirst: Boolean): LapStatus {
        if (!lap.complete) {
            return if (isFirst) LapStatus.OutLap else LapStatus.Invalid
        }
        if (lap.inPit) return LapStatus.PitIn
        if (lap.invalid) return LapStatus.Dirty
        if (isBest) return LapStatus.BestLap
        return LapStatus.Clean
    }

    private fun buildHeader(details: RecordedSessionDetail): SessionDetailHeaderUi {
        val summary = details.summary
        val dateLabel = dateFormatter.format(Instant.ofEpochMilli(summary.startedAtMs).atZone(zoneId))
        val timeLabel = timeFormatter.format(Instant.ofEpochMilli(summary.startedAtMs).atZone(zoneId))
        val trackLabel = summary.trackId.orUnknownLabel()
        val carLabel = summary.carModel.orUnknownLabel()
        val airTemp = summary.airTempC.formatTemperatureLabel()
        val trackTemp = summary.trackTempC.formatTemperatureLabel()

        val chips = listOf(
            "Air: $airTemp / Track: $trackTemp",
            carLabel,
            trackLabel,
        )

        return SessionDetailHeaderUi(
            title = "Session",
            subtitle = "$dateLabel, $timeLabel",
            chips = chips
        )
    }

    private fun buildFilter(
        label: String,
        selectedId: String,
        options: List<DropdownOptionUi>,
    ): DropdownFilterUi {
        val resolved = if (options.any { it.id == selectedId }) selectedId else options.first().id
        val selectedLabel = options.firstOrNull { it.id == resolved }?.label ?: options.first().label
        return DropdownFilterUi(
            label = label,
            selectedId = resolved,
            selectedLabel = selectedLabel,
            options = options
        )
    }

    private fun String?.orUnknownLabel(): String = this?.takeIf { it.isNotBlank() } ?: "Unknown"

    private fun Int?.formatSectorMs(): String {
        if (this == null) return "--.--"
        return String.format(Locale.US, "%.3f", this / 1000.0)
    }

    private fun Float?.formatTemperatureLabel(): String {
        if (this == null || !this.isFinite()) return "--°C"
        return "${String.format(Locale.US, "%.0f", this)}°C"
    }

    private fun DropdownFilterUi.updateSelection(optionId: String): DropdownFilterUi =
        copy(selectedId = optionId, selectedLabel = options.firstOrNull { it.id == optionId }?.label ?: selectedLabel)

    private companion object {

        const val PAGE_SIZE = 10
    }
}
