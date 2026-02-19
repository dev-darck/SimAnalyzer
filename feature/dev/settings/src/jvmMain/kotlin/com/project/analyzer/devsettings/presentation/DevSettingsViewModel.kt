package com.project.analyzer.devsettings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.impl.compose.HudPreferences
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

@Inject
internal class DevSettingsViewModel(
    private val telemetryLifecycle: TelemetryLifecycle,
    private val hudPreferences: HudPreferences,
    private val panels: Provider<Set<HudPanel>>,
) : ViewModel() {

    private val _state = MutableStateFlow(DevSettingsState())
    val state: StateFlow<DevSettingsState> = _state.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    private var devPanels: List<HudPanel> = emptyList()
    private var lastVisibleIds: Set<String> = emptySet()
    private val telemetryOrder = mutableListOf<String>()
    private val telemetryValues = LinkedHashMap<String, String>()

    init {
        observeDevHudPanels()
        observeHudVisibility()
        observeTelemetryEvents()
        observeTelemetryFrames()
    }

    fun setDevHudPanelEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            val devIds = devPanels.map { it.id }.toSet()
            if (devIds.isEmpty() || id !in devIds) {
                val next = if (enabled) lastVisibleIds + id else lastVisibleIds - id
                hudPreferences.saveVisiblePanels(next)
                return@launch
            }

            val currentVisible = lastVisibleIds
            val activeDevIds = currentVisible.intersect(devIds)
            val nextDevIds = if (enabled) activeDevIds + id else activeDevIds - id

            if (activeDevIds.isEmpty() && enabled) {
                val backup = currentVisible - devIds
                hudPreferences.saveVisiblePanelsBackup(backup)
            }

            if (nextDevIds.isEmpty()) {
                val backup = hudPreferences.getVisiblePanelsBackup()
                val restored = if (backup.isNotEmpty()) backup else currentVisible - devIds
                hudPreferences.saveVisiblePanels(restored)
                hudPreferences.clearVisiblePanelsBackup()
            } else {
                hudPreferences.saveVisiblePanels(nextDevIds)
            }
        }
    }

    private fun observeDevHudPanels() {
        viewModelScope.launch {
            devPanels = panels.invoke()
                .filter { it.isDevOnly }
                .sortedBy { it.id }
            updateDevHudPanels()
        }
    }

    private fun observeHudVisibility() {
        viewModelScope.launch {
            hudPreferences.observeVisiblePanels().collect { visibleIds ->
                lastVisibleIds = visibleIds
                updateDevHudPanels()
            }
        }
        viewModelScope.launch {
            hudPreferences.observeHudEnabled().collect { enabled ->
                _state.update { it.copy(hud = it.hud.copy(hudEnabled = enabled)) }
            }
        }
    }

    private fun updateDevHudPanels() {
        val panelsUi = devPanels.map { panel ->
            DevHudPanelUi(
                id = panel.id,
                title = formatPanelTitle(panel.id),
                description = panel.description,
                enabled = panel.id in lastVisibleIds
            )
        }

        _state.update { current ->
            current.copy(hud = current.hud.copy(panels = panelsUi))
        }
    }

    private fun observeTelemetryEvents() {
        viewModelScope.launch {
            telemetryLifecycle.events.collect { event ->
                val label = when (event) {
                    TelemetryLifecycleEvent.SimConnected -> "Sim connected"
                    TelemetryLifecycleEvent.SimDisconnected -> "Sim disconnected"
                    is TelemetryLifecycleEvent.SessionStarted -> "Session started"
                    is TelemetryLifecycleEvent.SessionUpdated -> "Session updated"
                    is TelemetryLifecycleEvent.SessionPaused -> "Session paused"
                    is TelemetryLifecycleEvent.SessionResumed -> "Session resumed"
                    is TelemetryLifecycleEvent.SessionEnded -> "Session ended"
                    is TelemetryLifecycleEvent.LapStarted -> "Lap started"
                    is TelemetryLifecycleEvent.LapFinished -> "Lap finished"
                }
                _state.update { current ->
                    current.copy(telemetry = current.telemetry.copy(status = label))
                }

                if (event is TelemetryLifecycleEvent.SimDisconnected) {
                    telemetryOrder.clear()
                    telemetryValues.clear()
                    _state.update { current ->
                        current.copy(telemetry = current.telemetry.copy(entries = emptyList()))
                    }
                }
            }
        }
    }

    private fun observeTelemetryFrames() {
        viewModelScope.launch {
            telemetryLifecycle.frames
                .sample(TELEMETRY_SAMPLE_MS)
                .collect { frame ->
                    val session = frame.session
                    val track = session?.track
                    val car = session?.car
                    val status = session?.status?.name?.lowercase()?.replace('_', ' ')
                        ?: _state.value.telemetry.status
                    val sessionType = session?.sessionType?.name?.lowercase()?.replace('_', ' ') ?: "-"
                    val trackLabel = listOfNotNull(track?.trackName, track?.trackId)
                        .joinToString(" / ")
                        .ifBlank { "-" }
                    val carLabel = car?.carModel ?: "-"
                    val updatedAt = formatTimestamp(System.currentTimeMillis())

                    val mappedEntries = TelemetryInspectorMapper.map(frame)
                    updateTelemetryEntries(mappedEntries)

                    _state.update { current ->
                        current.copy(
                            telemetry = current.telemetry.copy(
                                status = status,
                                sessionType = sessionType,
                                trackLabel = trackLabel,
                                carLabel = carLabel,
                                frameId = frame.frameId,
                                lastUpdatedLabel = updatedAt,
                            )
                        )
                    }
                }
        }
    }

    private fun updateTelemetryEntries(entries: Map<String, String>) {
        entries.forEach { (path, value) ->
            if (!telemetryValues.containsKey(path)) {
                telemetryOrder.add(path)
            }
            telemetryValues[path] = value
        }

        telemetryValues.keys
            .filter { it !in entries }
            .forEach { missingKey -> telemetryValues[missingKey] = "<missing>" }

        val ordered = telemetryOrder.mapNotNull { path ->
            telemetryValues[path]?.let { TelemetryEntry(path, it) }
        }

        _state.update { current ->
            current.copy(telemetry = current.telemetry.copy(entries = ordered))
        }
    }

    private fun formatTimestamp(epochMs: Long): String =
        timeFormatter.format(Instant.ofEpochMilli(epochMs))

    private fun formatPanelTitle(id: String): String {
        return id.split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private companion object {

        val TELEMETRY_SAMPLE_MS: Long = 250.milliseconds.inWholeMilliseconds
    }
}
