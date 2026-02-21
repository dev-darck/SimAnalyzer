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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.CalibrationHost
import com.project.analyzer.calibration.presentation.trackmap.TrackMapBuilderScreen
import com.project.analyzer.calibration.presentation.trackmap.TrackMapLibraryScreen
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import dev.zacsweers.metrox.viewmodel.metroViewModel

private enum class DevSettingsSection {
    Calibration,
    TrackMap,
    TrackMapLibrary,
    Telemetry,
    Hud,
}

private data class DevNavItem(val section: DevSettingsSection, val title: String, val subtitle: String)

private val devNavItems = listOf(
    DevNavItem(
        section = DevSettingsSection.Calibration,
        title = "Calibration",
        subtitle = "Capture start/finish and sector gates",
    ),
    DevNavItem(
        section = DevSettingsSection.TrackMap,
        title = "Track map",
        subtitle = "Capture and save the track layout",
    ),
    DevNavItem(
        section = DevSettingsSection.TrackMapLibrary,
        title = "Track maps",
        subtitle = "Browse saved track layouts",
    ),
    DevNavItem(
        section = DevSettingsSection.Telemetry,
        title = "Telemetry inspector",
        subtitle = "Live frame values without auto-scroll",
    ),
    DevNavItem(
        section = DevSettingsSection.Hud,
        title = "Calibration HUD",
        subtitle = "Debug overlays and minimap",
    ),
)

@Composable
internal fun DevSettingsScreen() {
    val viewModel: DevSettingsViewModel = metroViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(DevSettingsSection.Calibration) }

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
                    items = devNavItems,
                    selected = section,
                    onSelect = { section = it },
                )
                DevSettingsContent(
                    section = section,
                    state = state,
                    onToggleHudPanel = viewModel::setDevHudPanelEnabled,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DevSettingsNavColumn(
                    items = devNavItems,
                    selected = section,
                    onSelect = { section = it },
                    modifier = Modifier.widthIn(min = 250.dp, max = 320.dp),
                )
                DevSettingsContent(
                    section = section,
                    state = state,
                    onToggleHudPanel = viewModel::setDevHudPanelEnabled,
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
            text = "Developer settings",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 20.sp,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Calibration tools, telemetry diagnostics, and dev HUDs.",
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 12.sp,
            style = MaterialTheme.typography.labelMedium,
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
            fontSize = 14.sp,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.subtitle,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 12.sp,
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
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .onClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.title,
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 12.sp,
            style = MaterialTheme.typography.labelMedium,
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
            title = "Calibration HUD overlays",
            subtitle = "Dev-only panels for gate capture and telemetry debug.",
        )

        if (!state.hudEnabled) {
            SectionCard(
                title = "HUD disabled",
                subtitle = "Enable the telemetry HUD in Settings to make overlays visible.",
            )
        }

        SectionCard(title = "Panels") {
            if (state.panels.isEmpty()) {
                Text(
                    text = "No dev HUD panels registered.",
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    fontSize = 12.sp,
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
                text = panel.title,
                color = SimAnalyzerTheme.material.onSurface,
                fontSize = 14.sp,
                style = MaterialTheme.typography.titleMedium,
            )
            if (panel.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = panel.description,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    fontSize = 12.sp,
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
            fontSize = 16.sp,
            style = MaterialTheme.typography.titleMedium,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
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
            text = "Status: ${state.status}",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Session: ${state.sessionType}",
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Text(
            text = "Track: ${state.trackLabel}",
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Text(
            text = "Car: ${state.carLabel}",
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Frame: ${state.frameId ?: "-"}",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                text = "Fields: ${state.entries.size}",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                text = "Updated: ${state.lastUpdatedLabel}",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun TelemetryEntryRow(entry: TelemetryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SimAnalyzerTheme.material.surface.copy(alpha = 0.85f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.path,
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.6f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = entry.value,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.4f),
        )
    }
}

@Preview
@Composable
private fun DevSettingsScreenPreview() {
    val sample = DevSettingsState(
        telemetry = TelemetryInspectorState(
            status = "Sim connected",
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
                DevHudPanelUi("calibration_debug", "Calibration Debug", "Debug telemetry", true),
                DevHudPanelUi("calibration_minimap", "Calibration MiniMap", "Live minimap", false),
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
