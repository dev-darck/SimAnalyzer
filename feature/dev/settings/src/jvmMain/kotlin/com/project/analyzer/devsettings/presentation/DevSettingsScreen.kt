@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.devsettings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.CalibrationHost
import com.project.analyzer.calibration.presentation.trackmap.TrackMapBuilderScreen
import com.project.analyzer.calibration.presentation.trackmap.TrackMapLibraryScreen
import com.project.analyzer.feature.dev.settings.Res.*
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

private enum class DevSettingsSection {
    Calibration,
    TrackMap,
    TrackMapLibrary,
    Telemetry,
    Hud,
}

private data class DevNavItem(val section: DevSettingsSection, val title: String, val subtitle: String)

@Composable
internal fun DevSettingsScreen() {
    val viewModel: DevSettingsViewModel = metroViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch: (DevSettingsIntent) -> Unit = viewModel::dispatch
    var section by rememberSaveable { mutableStateOf(DevSettingsSection.Calibration) }
    val navItems = rememberDevNavItems()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .padding(16.dp),
    ) {
        val isCompact = maxWidth < 980.dp

        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DevSettingsHeader()
                DevSettingsNavRow(
                    items = navItems,
                    selected = section,
                    onSelect = { section = it },
                )
                DevSettingsContent(
                    section = section,
                    state = state,
                    onToggleHudPanel = { id, enabled ->
                        dispatch(DevSettingsIntent.ToggleHudPanel(id, enabled))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DevSettingsNavColumn(
                    items = navItems,
                    selected = section,
                    onSelect = { section = it },
                    modifier = Modifier.widthIn(min = 250.dp, max = 320.dp),
                )
                DevSettingsContent(
                    section = section,
                    state = state,
                    onToggleHudPanel = { id, enabled ->
                        dispatch(DevSettingsIntent.ToggleHudPanel(id, enabled))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DevSettingsHeader() {
    Column(
        modifier = Modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.dev_settings_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.dev_settings_subtitle),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun DevSettingsNavColumn(
    items: List<DevNavItem>,
    selected: DevSettingsSection,
    onSelect: (DevSettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DevSettingsHeader()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                DevSettingsNavCard(
                    item = item,
                    selected = item.section == selected,
                    onClick = { onSelect(item.section) },
                )
            }
        }
    }
}

@Composable
private fun DevSettingsNavRow(
    items: List<DevNavItem>,
    selected: DevSettingsSection,
    onSelect: (DevSettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            DevSettingsNavChip(
                item = item,
                selected = item.section == selected,
                onClick = { onSelect(item.section) },
            )
        }
    }
}

@Composable
private fun DevSettingsNavCard(
    item: DevNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) {
        SimAnalyzerTheme.material.primary.copy(alpha = 0.12f)
    } else {
        SimAnalyzerTheme.material.surface
    }
    val borderColor = if (selected) {
        SimAnalyzerTheme.material.primary.copy(alpha = 0.45f)
    } else {
        SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.4f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(background)
            .border(1.dp, borderColor, SimAnalyzerTheme.shapes.medium)
            .onClick(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            text = item.title,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.subtitle,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DevSettingsNavChip(
    item: DevNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) {
        SimAnalyzerTheme.material.primary.copy(alpha = 0.18f)
    } else {
        SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.35f)
    }
    val borderColor = if (selected) {
        SimAnalyzerTheme.material.primary.copy(alpha = 0.4f)
    } else {
        SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.3f)
    }

    Row(
        modifier = modifier
            .clip(SimAnalyzerTheme.corners.pill)
            .background(background)
            .border(1.dp, borderColor, SimAnalyzerTheme.corners.pill)
            .onClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.title,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun DevSettingsContent(
    section: DevSettingsSection,
    state: DevSettingsState,
    onToggleHudPanel: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (section) {
            DevSettingsSection.Calibration -> CalibrationHost()

            DevSettingsSection.TrackMap -> TrackMapBuilderScreen()

            DevSettingsSection.TrackMapLibrary -> TrackMapLibraryScreen()

            DevSettingsSection.Telemetry -> TelemetryInspectorScreen(state = state.telemetry)

            DevSettingsSection.Hud -> DevHudSettingsScreen(
                state = state.hud,
                onToggleHudPanel = onToggleHudPanel,
            )
        }
    }
}

@Composable
private fun DevHudSettingsScreen(
    state: DevHudState,
    onToggleHudPanel: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(
            title = stringResource(Res.string.dev_settings_hud_title),
            subtitle = stringResource(Res.string.dev_settings_hud_subtitle),
        )

        if (!state.hudEnabled) {
            SectionCard(
                title = stringResource(Res.string.dev_settings_hud_disabled_title),
                subtitle = stringResource(Res.string.dev_settings_hud_disabled_subtitle),
            )
        }

        SectionCard(title = stringResource(Res.string.dev_settings_hud_panels_title)) {
            if (state.panels.isEmpty()) {
                Text(
                    text = stringResource(Res.string.dev_settings_hud_no_panels),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.panels.forEach { panel ->
                        DevHudPanelRow(
                            panel = panel,
                            onToggle = { enabled -> onToggleHudPanel(panel.id, enabled) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevHudPanelRow(panel: DevHudPanelUi, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.22f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = panel.displayTitle(),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleMedium,
            )
            val description = panel.displayDescription()
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                )
            }
        }
        Switch(
            checked = panel.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SimAnalyzerTheme.material.onSurface,
                checkedTrackColor = SimAnalyzerTheme.material.primary,
                uncheckedThumbColor = SimAnalyzerTheme.material.onSurfaceVariant,
                uncheckedTrackColor = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.25f),
            ),
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
        if (content != null) {
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun TelemetryInspectorScreen(state: TelemetryInspectorState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        TelemetryInspectorHeader(state)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(state.entries, key = { it.path }) { entry ->
                TelemetryEntryRow(entry = entry)
            }
        }
    }
}

@Composable
private fun TelemetryInspectorHeader(state: TelemetryInspectorState) {
    Column(
        modifier = Modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.dev_settings_telemetry_status, state.status.asText()),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.dev_settings_telemetry_session, state.sessionType),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(Res.string.dev_settings_telemetry_track, state.trackLabel),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(Res.string.dev_settings_telemetry_car, state.carLabel),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.dev_settings_telemetry_frame, state.frameId?.toString() ?: "-"),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.dev_settings_telemetry_fields, state.entries.size),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.dev_settings_telemetry_updated, state.lastUpdatedLabel),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun rememberDevNavItems(): List<DevNavItem> = listOf(
    DevNavItem(
        section = DevSettingsSection.Calibration,
        title = stringResource(Res.string.dev_settings_nav_calibration_title),
        subtitle = stringResource(Res.string.dev_settings_nav_calibration_subtitle),
    ),
    DevNavItem(
        section = DevSettingsSection.TrackMap,
        title = stringResource(Res.string.dev_settings_nav_track_map_title),
        subtitle = stringResource(Res.string.dev_settings_nav_track_map_subtitle),
    ),
    DevNavItem(
        section = DevSettingsSection.TrackMapLibrary,
        title = stringResource(Res.string.dev_settings_nav_track_maps_title),
        subtitle = stringResource(Res.string.dev_settings_nav_track_maps_subtitle),
    ),
    DevNavItem(
        section = DevSettingsSection.Telemetry,
        title = stringResource(Res.string.dev_settings_nav_telemetry_title),
        subtitle = stringResource(Res.string.dev_settings_nav_telemetry_subtitle),
    ),
    DevNavItem(
        section = DevSettingsSection.Hud,
        title = stringResource(Res.string.dev_settings_nav_hud_title),
        subtitle = stringResource(Res.string.dev_settings_nav_hud_subtitle),
    ),
)

@Composable
private fun DevHudPanelUi.displayTitle(): String = when (id) {
    "calibration_debug" -> stringResource(Res.string.dev_settings_hud_panel_calibration_debug_title)
    "calibration_minimap" -> stringResource(Res.string.dev_settings_hud_panel_calibration_minimap_title)
    "track_map_builder" -> stringResource(Res.string.dev_settings_hud_panel_track_map_builder_title)
    else -> id.toDisplayLabel()
}

@Composable
private fun DevHudPanelUi.displayDescription(): String? = when (id) {
    "calibration_debug" -> stringResource(Res.string.dev_settings_hud_panel_calibration_debug_description)
    "calibration_minimap" -> stringResource(Res.string.dev_settings_hud_panel_calibration_minimap_description)
    "track_map_builder" -> stringResource(Res.string.dev_settings_hud_panel_track_map_builder_description)
    else -> null
}

@Composable
private fun TelemetryEntryRow(entry: TelemetryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.corners.item)
            .background(SimAnalyzerTheme.material.surface.copy(alpha = 0.85f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.path,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.bodySmall,
            modifier = Modifier.weight(0.6f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = entry.value,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
            modifier = Modifier.weight(0.4f),
        )
    }
}

@Preview
@Composable
private fun DevSettingsScreenPreview() {
    val sample = DevSettingsState(
        telemetry = TelemetryInspectorState(
            status = TelemetryStatusUi.SimConnected,
            sessionType = "practice",
            trackLabel = "Monza",
            carLabel = "BMW",
            frameId = 128,
            lastUpdatedLabel = "12:00:00.000",
            entries = listOf(TelemetryEntry("frame.car.speedKmh", "120")),
        ),
        hud = DevHudState(
            hudEnabled = true,
            panels = listOf(
                DevHudPanelUi("calibration_debug", true),
                DevHudPanelUi("calibration_minimap", false),
            ),
        ),
    )

    SimAnalyzerTheme {
        DevHudSettingsScreen(
            state = sample.hud,
            onToggleHudPanel = { _, _ -> },
        )
    }
}

@Composable
private fun TelemetryStatusUi.asText(): String = when (this) {
    TelemetryStatusUi.WaitingForTelemetry -> stringResource(Res.string.dev_settings_status_waiting_for_telemetry)
    TelemetryStatusUi.SimConnected -> stringResource(Res.string.dev_settings_status_sim_connected)
    TelemetryStatusUi.SimDisconnected -> stringResource(Res.string.dev_settings_status_sim_disconnected)
    TelemetryStatusUi.SessionStarted -> stringResource(Res.string.dev_settings_status_session_started)
    TelemetryStatusUi.SessionUpdated -> stringResource(Res.string.dev_settings_status_session_updated)
    TelemetryStatusUi.SessionPaused -> stringResource(Res.string.dev_settings_status_session_paused)
    TelemetryStatusUi.SessionResumed -> stringResource(Res.string.dev_settings_status_session_resumed)
    TelemetryStatusUi.SessionEnded -> stringResource(Res.string.dev_settings_status_session_ended)
    TelemetryStatusUi.LapStarted -> stringResource(Res.string.dev_settings_status_lap_started)
    TelemetryStatusUi.LapFinished -> stringResource(Res.string.dev_settings_status_lap_finished)
    is TelemetryStatusUi.Raw -> value
}

private fun String.toDisplayLabel(): String = split('_', '-')
    .filter { it.isNotBlank() }
    .joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
