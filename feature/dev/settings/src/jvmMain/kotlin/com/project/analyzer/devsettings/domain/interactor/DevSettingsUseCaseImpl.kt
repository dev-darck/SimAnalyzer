package com.project.analyzer.devsettings.domain.interactor

import com.project.analyzer.devsettings.presentation.DevHudPanelUi
import com.project.analyzer.devsettings.presentation.DevSettingsState
import com.project.analyzer.devsettings.presentation.TelemetryEntry
import com.project.analyzer.devsettings.presentation.TelemetryInspectorMapper
import com.project.analyzer.devsettings.presentation.TelemetryStatusUi
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.HudPreferencesStore
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.CoroutineScope
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
class DevSettingsUseCaseImpl(
    private val telemetryLifecycle: TelemetryLifecycle,
    private val hudPreferencesStore: HudPreferencesStore,
    private val panels: Provider<Set<HudPanel>>,
) : DevSettingsUseCase {

    private val _state = MutableStateFlow(DevSettingsState())
    override val state: StateFlow<DevSettingsState> = _state.asStateFlow()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())

    private var started: Boolean = false
    private var devPanels: List<HudPanel> = emptyList()
    private var lastVisibleIds: Set<String> = emptySet()
    private val telemetryOrder = mutableListOf<String>()
    private val telemetryValues = LinkedHashMap<String, String>()

    override fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        observeDevHudPanels(scope)
        observeHudVisibility(scope)
        observeTelemetryEvents(scope)
        observeTelemetryFrames(scope)
    }

    override suspend fun setDevHudPanelEnabled(id: String, enabled: Boolean) {
        val devIds = devPanels.map { it.id }.toSet()
        if (devIds.isEmpty() || id !in devIds) {
            val next = if (enabled) lastVisibleIds + id else lastVisibleIds - id
            hudPreferencesStore.saveVisiblePanels(next)
            return
        }

        val currentVisible = lastVisibleIds
        val activeDevIds = currentVisible.intersect(devIds)
        val nextDevIds = if (enabled) activeDevIds + id else activeDevIds - id

        if (activeDevIds.isEmpty() && enabled) {
            val backup = currentVisible - devIds
            hudPreferencesStore.saveVisiblePanelsBackup(backup)
        }

        if (nextDevIds.isEmpty()) {
            val backup = hudPreferencesStore.getVisiblePanelsBackup()
            val restored = if (backup.isNotEmpty()) backup else currentVisible - devIds
            hudPreferencesStore.saveVisiblePanels(restored)
            hudPreferencesStore.clearVisiblePanelsBackup()
        } else {
            hudPreferencesStore.saveVisiblePanels(nextDevIds)
        }
    }

    private fun observeDevHudPanels(scope: CoroutineScope) {
        scope.launch {
            devPanels = panels.invoke()
                .filter { it.isDevOnly }
                .sortedBy { it.id }
            updateDevHudPanels()
        }
    }

    private fun observeHudVisibility(scope: CoroutineScope) {
        scope.launch {
            hudPreferencesStore.observeVisiblePanels().collect { visibleIds ->
                lastVisibleIds = visibleIds
                updateDevHudPanels()
            }
        }
        scope.launch {
            hudPreferencesStore.observeHudEnabled().collect { enabled ->
                _state.update { it.copy(hud = it.hud.copy(hudEnabled = enabled)) }
            }
        }
    }

    private fun updateDevHudPanels() {
        val panelsUi = devPanels.map { panel ->
            DevHudPanelUi(
                id = panel.id,
                enabled = panel.id in lastVisibleIds,
            )
        }

        _state.update { current ->
            current.copy(hud = current.hud.copy(panels = panelsUi))
        }
    }

    private fun observeTelemetryEvents(scope: CoroutineScope) {
        scope.launch {
            telemetryLifecycle.events.collect { event ->
                val label = when (event) {
                    TelemetryLifecycleEvent.SimConnected -> TelemetryStatusUi.SimConnected
                    TelemetryLifecycleEvent.SimDisconnected -> TelemetryStatusUi.SimDisconnected
                    is TelemetryLifecycleEvent.SessionStarted -> TelemetryStatusUi.SessionStarted
                    is TelemetryLifecycleEvent.SessionUpdated -> TelemetryStatusUi.SessionUpdated
                    is TelemetryLifecycleEvent.SessionPaused -> TelemetryStatusUi.SessionPaused
                    is TelemetryLifecycleEvent.SessionResumed -> TelemetryStatusUi.SessionResumed
                    is TelemetryLifecycleEvent.SessionEnded -> TelemetryStatusUi.SessionEnded
                    is TelemetryLifecycleEvent.LapStarted -> TelemetryStatusUi.LapStarted
                    is TelemetryLifecycleEvent.LapFinished -> TelemetryStatusUi.LapFinished
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

    private fun observeTelemetryFrames(scope: CoroutineScope) {
        scope.launch {
            telemetryLifecycle.frames
                .sample(TELEMETRY_SAMPLE_MS)
                .collect { frame ->
                    val session = frame.session
                    val track = session?.track
                    val car = session?.car
                    val status = session?.status?.name
                        ?.lowercase()
                        ?.replace('_', ' ')
                        ?.let(TelemetryStatusUi::Raw)
                        ?: _state.value.telemetry.status
                    val sessionType = session?.sessionType?.name?.lowercase()?.replace('_', ' ') ?: "-"
                    val trackLabel = listOfNotNull(track?.trackName, track?.trackId)
                        .joinToString(" / ")
                        .ifBlank { "-" }
                    val carLabel = listOfNotNull(car?.carName, car?.carModel)
                        .joinToString(" / ")
                        .ifBlank { "-" }
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
                            ),
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

    private fun formatTimestamp(epochMs: Long): String = timeFormatter.format(Instant.ofEpochMilli(epochMs))

    private companion object {

        val TELEMETRY_SAMPLE_MS: Long = 250.milliseconds.inWholeMilliseconds
    }
}
