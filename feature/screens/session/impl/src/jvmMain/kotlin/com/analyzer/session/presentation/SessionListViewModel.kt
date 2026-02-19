package com.analyzer.session.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.presentation.model.DropdownFilterUi
import com.analyzer.session.presentation.model.DropdownOptionUi
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.analyzer.session.presentation.model.SessionStatsUi
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
class SessionListViewModel(
    private val repository: RecordedSessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionListState())
    val state: StateFlow<SessionListState> = _state.asStateFlow()

    private var allSessions: List<RecordedSessionSummary> = emptyList()

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val zoneId = ZoneId.systemDefault()

    init {
        refresh()
    }

    fun dispatch(intent: SessionListIntent) {
        when (intent) {
            SessionListIntent.Refresh -> refresh()
            is SessionListIntent.ChangeGame -> updateFilter { copy(gameFilter = gameFilter.updateSelection(intent.optionId)) }
            is SessionListIntent.ChangeTrack -> updateFilter { copy(trackFilter = trackFilter.updateSelection(intent.optionId)) }
            is SessionListIntent.ChangeCar -> updateFilter { copy(carFilter = carFilter.updateSelection(intent.optionId)) }
            is SessionListIntent.ChangeDate -> updateFilter { copy(dateFilter = dateFilter.updateSelection(intent.optionId)) }
            is SessionListIntent.ChangeSort -> updateFilter { copy(sortFilter = sortFilter.updateSelection(intent.optionId)) }
            is SessionListIntent.ChangeSearch -> updateFilter { copy(searchQuery = intent.query, page = 1) }
            is SessionListIntent.ChangePage -> updateFilter { copy(page = intent.page) }
            is SessionListIntent.SaveSession -> saveSession(intent.sessionId)
            is SessionListIntent.DeleteSession -> deleteSession(intent.sessionId)
        }
    }

    private fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isLoading = true, error = null) }
            } else {
                _state.update { it.copy(error = null) }
            }
            val sessions = repository.loadSessions()
            allSessions = sessions
            val updated = rebuildFilters(_state.value, sessions)
            _state.value = applyFilters(updated, sessions)
        }
    }

    private fun updateFilter(mutator: SessionListState.() -> SessionListState) {
        val updated = mutator(_state.value)
        _state.value = applyFilters(updated, allSessions)
    }

    private fun saveSession(sessionId: Long) {
        viewModelScope.launch {
            val saved = repository.saveSession(sessionId)
            if (saved) {
                refresh(showLoading = false)
            } else {
                _state.update { it.copy(error = "Failed to save session.") }
            }
        }
    }

    private fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            val deleted = repository.deleteSession(sessionId)
            if (deleted) {
                refresh(showLoading = false)
            } else {
                _state.update { it.copy(error = "Failed to delete session.") }
            }
        }
    }

    private fun rebuildFilters(state: SessionListState, sessions: List<RecordedSessionSummary>): SessionListState {
        val gameOptions = buildGameOptions(sessions)
        val trackOptions = buildOptions(sessions.map { it.trackId.orUnknownLabel() })
        val carOptions = buildOptions(sessions.map { it.carModel.orUnknownLabel() })
        val dateOptions = buildOptions(sessions.map { formatDate(it.startedAtMs) })
        val sortOptions = listOf(
            DropdownOptionUi("best", "Best lap"),
            DropdownOptionUi("newest", "Newest"),
            DropdownOptionUi("oldest", "Oldest"),
        )

        return state.copy(
            gameFilter = buildFilter("Game", state.gameFilter.selectedId, gameOptions),
            trackFilter = buildFilter("Track", state.trackFilter.selectedId, trackOptions),
            carFilter = buildFilter("Car", state.carFilter.selectedId, carOptions),
            dateFilter = buildFilter("Date", state.dateFilter.selectedId, dateOptions),
            sortFilter = buildFilter("Sort by", state.sortFilter.selectedId, sortOptions),
            stats = buildStats(sessions)
        )
    }

    private fun applyFilters(state: SessionListState, sessions: List<RecordedSessionSummary>): SessionListState {
        val searchQuery = state.searchQuery.trim().lowercase(Locale.US)
        val filtered = sessions.filter { session ->
            val gameId = normalizeGameId(session.gameId)
            val gameLabel = gameLabel(gameId)
            val trackLabel = session.trackId.orUnknownLabel()
            val carLabel = session.carModel.orUnknownLabel()
            val dateLabel = formatDate(session.startedAtMs)

            val gameMatch = state.gameFilter.isAllOrSelected(gameId)
            val trackMatch = state.trackFilter.isAllOrSelected(trackLabel)
            val carMatch = state.carFilter.isAllOrSelected(carLabel)
            val dateMatch = state.dateFilter.isAllOrSelected(dateLabel)
            val searchMatch = searchQuery.isBlank() ||
                trackLabel.lowercase(Locale.US).contains(searchQuery) ||
                carLabel.lowercase(Locale.US).contains(searchQuery) ||
                gameLabel.lowercase(Locale.US).contains(searchQuery) ||
                gameId.contains(searchQuery)

            gameMatch && trackMatch && carMatch && dateMatch && searchMatch
        }

        val sorted = when (state.sortFilter.selectedId) {
            "newest" -> filtered.sortedByDescending { it.startedAtMs }
            "oldest" -> filtered.sortedBy { it.startedAtMs }
            else -> filtered.sortedBy { it.bestLapTimeMs ?: Int.MAX_VALUE }
        }

        val rows = sorted.map { session -> session.toRow() }
        val pageCount = maxOf(1, ceil(rows.size / PAGE_SIZE.toDouble()).toInt())
        val page = state.page.coerceIn(1, pageCount)
        val visible = rows.drop((page - 1) * PAGE_SIZE).take(PAGE_SIZE)

        return state.copy(
            isLoading = false,
            error = if (rows.isEmpty() && sessions.isNotEmpty()) "No sessions match filters." else null,
            page = page,
            pageCount = pageCount,
            sessions = rows,
            visibleSessions = visible
        )
    }

    private fun buildFilter(
        label: String,
        selectedId: String,
        options: List<DropdownOptionUi>,
    ): DropdownFilterUi {
        val resolved = if (options.any { it.id == selectedId }) selectedId else FILTER_ALL_ID
        val selectedLabel = options.firstOrNull { it.id == resolved }?.label ?: options.first().label
        return DropdownFilterUi(
            label = label,
            selectedId = resolved,
            selectedLabel = selectedLabel,
            options = options
        )
    }

    private fun buildOptions(values: List<String>): List<DropdownOptionUi> {
        val unique = values
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val options = mutableListOf(DropdownOptionUi(FILTER_ALL_ID, "All"))
        unique.forEach { value ->
            options.add(DropdownOptionUi(value, value))
        }
        return options
    }

    private fun buildGameOptions(sessions: List<RecordedSessionSummary>): List<DropdownOptionUi> {
        val unique = sessions
            .map { normalizeGameId(it.gameId) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val options = mutableListOf(DropdownOptionUi(FILTER_ALL_ID, "All"))
        unique.forEach { value ->
            options.add(DropdownOptionUi(value, gameLabel(value)))
        }
        return options
    }

    private fun buildStats(sessions: List<RecordedSessionSummary>): SessionStatsUi {
        val totalDistance = sessions.sumOf { it.distanceKm }
        val incidents = sessions.sumOf { it.totalIncidents }
        val favoriteCar = sessions
            .mapNotNull { it.carModel?.takeIf { label -> label.isNotBlank() } }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
            ?: "-"

        return SessionStatsUi(
            totalDistanceLabel = String.format(Locale.US, "%.3f", totalDistance),
            sessionsCount = sessions.size,
            incidentsCount = incidents,
            favoriteCar = favoriteCar,
        )
    }

    private fun RecordedSessionSummary.toRow(): SessionRowUi {
        val dateLabel = formatDate(startedAtMs)
        val timeLabel = formatTime(startedAtMs)
        val gameLabel = gameLabel(gameId)
        val trackLabel = trackId.orUnknownLabel()
        val carLabel = carModel.orUnknownLabel()
        val lapLabel = lapCount.toString()
        val bestLapLabel = bestLapTimeMs?.fromMsToLapTime() ?: "0:00.000"

        return SessionRowUi(
            sessionId = sessionId,
            dateLabel = dateLabel,
            timeLabel = timeLabel,
            gameLabel = gameLabel,
            trackLabel = trackLabel,
            carLabel = carLabel,
            lapsLabel = lapLabel,
            bestLapLabel = bestLapLabel,
            isSaved = isSaved,
        )
    }

    private fun formatDate(epochMs: Long): String =
        dateFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun formatTime(epochMs: Long): String =
        timeFormatter.format(Instant.ofEpochMilli(epochMs).atZone(zoneId))

    private fun String?.orUnknownLabel(): String = this?.takeIf { it.isNotBlank() } ?: "Unknown"

    private fun DropdownFilterUi.isAllOrSelected(value: String): Boolean =
        selectedId == FILTER_ALL_ID || selectedId == value

    private fun DropdownFilterUi.updateSelection(optionId: String): DropdownFilterUi =
        copy(selectedId = optionId, selectedLabel = options.firstOrNull { it.id == optionId }?.label ?: selectedLabel)

    private fun normalizeGameId(gameId: String): String =
        gameId.trim().lowercase(Locale.US).ifBlank { "unknown" }

    private fun gameLabel(gameId: String): String {
        return when (val normalized = normalizeGameId(gameId)) {
            "ac" -> "AC"
            "ace" -> "AC Evo"
            "acc" -> "ACC"
            "lmu" -> "LMU"
            "unknown" -> "Unknown"
            else -> normalized.uppercase(Locale.US)
        }
    }

    private companion object {

        const val PAGE_SIZE = 8
    }
}
