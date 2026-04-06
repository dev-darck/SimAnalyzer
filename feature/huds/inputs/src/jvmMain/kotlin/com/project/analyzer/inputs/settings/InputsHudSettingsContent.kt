@file:Suppress("WildcardImport", "NoWildcardImports")

@file:OptIn(ExperimentalFoundationApi::class)

package com.project.analyzer.inputs.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.huds.inputs.Res.Res
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_brake_line
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_clutch_line
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_graph_height
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_graph_height_tooltip
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_header
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_history
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_history_tooltip
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_history_value
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_legend
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_steering_line
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_throttle_line
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_value_suffix_dp
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_width
import com.project.analyzer.feature.huds.inputs.Res.inputs_settings_width_tooltip
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.slider.SettingsIntSliderRow
import com.project.analyzer.ui.tooltip.Tooltip
import org.jetbrains.compose.resources.stringResource

private val options = listOf(1, 2, 3)

@Composable
internal fun InputsHudSettingsContent(
    settings: InputHudSettings,
    modifier: Modifier = Modifier,
    onChange: (InputHudSettings) -> Unit = {},
) {
    var draft by remember(settings) { mutableStateOf(settings) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingSwitch(
            title = stringResource(Res.string.inputs_settings_throttle_line),
            checked = settings.showThrottle,
        ) { v ->
            onChange(settings.copy(showThrottle = v))
        }
        SettingSwitch(
            title = stringResource(Res.string.inputs_settings_brake_line),
            checked = settings.showBrake,
        ) { v ->
            onChange(settings.copy(showBrake = v))
        }
        SettingSwitch(
            title = stringResource(Res.string.inputs_settings_clutch_line),
            checked = settings.showClutch,
        ) { v ->
            onChange(settings.copy(showClutch = v))
        }
        SettingSwitch(
            title = stringResource(Res.string.inputs_settings_steering_line),
            checked = settings.showSteer,
        ) { v ->
            onChange(settings.copy(showSteer = v))
        }

        Spacer(Modifier.height(6.dp))

        SettingSwitch(title = stringResource(Res.string.inputs_settings_header), checked = settings.showHeader) { v ->
            onChange(settings.copy(showHeader = v))
        }
        SettingSwitch(title = stringResource(Res.string.inputs_settings_legend), checked = settings.showLegend) { v ->
            onChange(settings.copy(showLegend = v))
        }

        Spacer(Modifier.height(6.dp))

        SettingsIntSliderRow(
            title = stringResource(Res.string.inputs_settings_width),
            tooltip = stringResource(Res.string.inputs_settings_width_tooltip),
            value = draft.widthDp,
            range = 260..900,
            step = 10,
            valueSuffix = stringResource(Res.string.inputs_settings_value_suffix_dp),
            onPreviewChange = { v -> draft = draft.copy(widthDp = v) },
            onCommit = { onChange(draft) },
        )

        SettingsIntSliderRow(
            title = stringResource(Res.string.inputs_settings_graph_height),
            tooltip = stringResource(Res.string.inputs_settings_graph_height_tooltip),
            value = draft.graphHeightDp,
            range = 60..120,
            step = 10,
            valueSuffix = stringResource(Res.string.inputs_settings_value_suffix_dp),
            onPreviewChange = { v -> draft = draft.copy(graphHeightDp = v) },
            onCommit = { onChange(draft) },
        )

        HistoryPicker(
            valueSec = draft.historySeconds,
            onSelect = { sec ->
                draft = draft.copy(historySeconds = sec)
                onChange(draft)
            },
        )
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
        )
    }
}

@Composable
private fun HistoryPicker(valueSec: Int, onSelect: (Int) -> Unit) {
    Column {
        Tooltip(
            tooltip = stringResource(Res.string.inputs_settings_history_tooltip),
        ) {
            Text(
                text = stringResource(Res.string.inputs_settings_history),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            options.forEach { sec ->
                val selected = sec == valueSec
                TextButton(
                    onClick = { onSelect(sec) },
                ) {
                    Text(
                        text = stringResource(Res.string.inputs_settings_history_value, sec.toString()),
                        color = if (selected) {
                            SimAnalyzerTheme.material.primary
                        } else {
                            SimAnalyzerTheme.material.onSurfaceVariant
                        },
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun InputsHudSettingsContentPreview() {
    SimAnalyzerTheme {
        InputsHudSettingsContent(
            settings = InputHudSettings(),
        )
    }
}
