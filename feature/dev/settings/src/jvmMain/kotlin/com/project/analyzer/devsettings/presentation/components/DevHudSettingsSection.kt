package com.project.analyzer.devsettings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.project.analyzer.devsettings.presentation.DevHudPanelUi
import com.project.analyzer.devsettings.presentation.DevHudState
import com.project.analyzer.feature.dev.settings.Res.Res
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_disabled_subtitle
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_disabled_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_no_panels
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_panel_calibration_debug_description
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_panel_calibration_debug_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_panel_calibration_minimap_description
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_panel_calibration_minimap_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_panel_track_map_builder_description
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_panel_track_map_builder_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_panels_title
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_subtitle
import com.project.analyzer.feature.dev.settings.Res.dev_settings_hud_title
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DevHudSettingsScreen(
    state: DevHudState,
    onToggleHudPanel: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DevSettingsSectionCard(
                title = stringResource(Res.string.dev_settings_hud_title),
                subtitle = stringResource(Res.string.dev_settings_hud_subtitle),
            )

            if (!state.hudEnabled) {
                DevSettingsSectionCard(
                    title = stringResource(Res.string.dev_settings_hud_disabled_title),
                    subtitle = stringResource(Res.string.dev_settings_hud_disabled_subtitle),
                )
            }

            DevSettingsSectionCard(
                title = stringResource(Res.string.dev_settings_hud_panels_title),
                showContent = true,
            ) {
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
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 6.dp),
            adapter = AppScrollbarAdapter(rememberScrollbarAdapter(scrollState)),
        )
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

private fun String.toDisplayLabel(): String = split('_', '-')
    .filter { it.isNotBlank() }
    .joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
