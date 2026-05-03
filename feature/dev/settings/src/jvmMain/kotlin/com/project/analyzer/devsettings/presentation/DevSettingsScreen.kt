@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.devsettings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.setup.CalibrationScreen
import com.project.analyzer.calibration.presentation.verify.CalibrationVerifyScreen
import com.project.analyzer.devsettings.presentation.components.DevHudSettingsScreen
import com.project.analyzer.devsettings.presentation.components.TelemetryInspectorScreen
import com.project.analyzer.feature.dev.settings.Res.Res
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_calibration_subtitle
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_calibration_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_hud_subtitle
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_hud_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_telemetry_subtitle
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_telemetry_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_track_map_subtitle
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_track_map_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_track_maps_subtitle
import com.project.analyzer.feature.dev.settings.Res.dev_settings_nav_track_maps_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_subtitle
import com.project.analyzer.feature.dev.settings.Res.dev_settings_title
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import com.project.analyzer.ui.scrollbar.AppHorizontalScrollbar
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

private enum class DevSettingsSection(val route: Route.SettingsRoot) {
    Calibration(Route.SettingsRoot.DevCalibration),
    TrackMap(Route.SettingsRoot.DevTrackMap),
    TrackMapLibrary(Route.SettingsRoot.DevTrackMapLibrary),
    Telemetry(Route.SettingsRoot.DevTelemetry),
    Hud(Route.SettingsRoot.DevHud),
}

private data class DevNavItem(val section: DevSettingsSection, val title: String, val subtitle: String)

@Composable
internal fun DevSettingsScreen(route: Route.SettingsRoot) {
    val viewModel: DevSettingsViewModel = metroViewModel()
    val navigator = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch: (DevSettingsIntent) -> Unit = viewModel::dispatch
    val section = route.toDevSettingsSection()
    val navItems = devSettingsNavItems()

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
                    onSelect = { navigator.navigate(it.route) },
                )
                DevSettingsContent(
                    route = route,
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
                    onSelect = { navigator.navigate(it.route) },
                    modifier = Modifier.widthIn(min = 250.dp, max = 320.dp),
                )
                DevSettingsContent(
                    route = route,
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
    items: ImmutableList<DevNavItem>,
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
    items: ImmutableList<DevNavItem>,
    selected: DevSettingsSection,
    onSelect: (DevSettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalScrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
                .padding(bottom = 10.dp),
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
        AppHorizontalScrollbar(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(end = 6.dp),
            adapter = AppScrollbarAdapter(
                rememberScrollbarAdapter(horizontalScrollState),
            ),
        )
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
    route: Route.SettingsRoot,
    state: DevSettingsState,
    onToggleHudPanel: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current

    Box(modifier = modifier) {
        when (route) {
            Route.SettingsRoot.DevSettings,
            Route.SettingsRoot.DevCalibration,
            Route.SettingsRoot.Settings,
            Route.SettingsRoot.HudSettings,
            -> CalibrationScreen(
                onVerify = { trackId ->
                    navigator.navigate(Route.SettingsRoot.DevCalibrationVerify(trackId))
                },
            )

            is Route.SettingsRoot.DevCalibrationVerify -> CalibrationVerifyScreen(
                trackId = route.trackId,
                onBack = navigator::handleBack,
            )

            Route.SettingsRoot.DevTelemetry -> TelemetryInspectorScreen(state = state.telemetry)

            Route.SettingsRoot.DevHud -> DevHudSettingsScreen(
                state = state.hud,
                onToggleHudPanel = onToggleHudPanel,
            )

            Route.SettingsRoot.DevTrackMap,
            Route.SettingsRoot.DevTrackMapLibrary,
            is Route.SettingsRoot.TrackMapCalibrationEditor,
            -> Unit
        }
    }
}

@Composable
private fun devSettingsNavItems(): ImmutableList<DevNavItem> = persistentListOf(
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

@Preview
@Composable
internal fun DevSettingsScreenPreview() {
    val sample = DevSettingsState(
        telemetry = TelemetryInspectorState(
            status = TelemetryStatusUi.SimConnected,
            sessionType = "practice",
            trackLabel = "Monza",
            carLabel = "BMW",
            frameId = 128,
            lastUpdatedLabel = "12:00:00.000",
            entries = persistentListOf(TelemetryEntry("frame.car.speedKmh", "120")),
        ),
        hud = DevHudState(
            hudEnabled = true,
            panels = persistentListOf(
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

private fun Route.SettingsRoot.toDevSettingsSection(): DevSettingsSection = when (this) {
    Route.SettingsRoot.DevSettings,
    Route.SettingsRoot.DevCalibration,
    is Route.SettingsRoot.DevCalibrationVerify,
    -> DevSettingsSection.Calibration

    Route.SettingsRoot.DevTrackMap -> DevSettingsSection.TrackMap

    Route.SettingsRoot.DevTrackMapLibrary -> DevSettingsSection.TrackMapLibrary

    is Route.SettingsRoot.TrackMapCalibrationEditor -> DevSettingsSection.TrackMapLibrary

    Route.SettingsRoot.DevTelemetry -> DevSettingsSection.Telemetry

    Route.SettingsRoot.DevHud -> DevSettingsSection.Hud

    Route.SettingsRoot.Settings,
    Route.SettingsRoot.HudSettings,
    -> DevSettingsSection.Calibration
}
