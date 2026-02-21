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
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.slider.SettingsIntSliderRow
import com.project.analyzer.ui.tooltip.Tooltip

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
        SettingSwitch(title = "Throttle line", checked = settings.showThrottle) { v ->
            onChange(settings.copy(showThrottle = v))
        }
        SettingSwitch(title = "Brake line", checked = settings.showBrake) { v ->
            onChange(settings.copy(showBrake = v))
        }
        SettingSwitch(title = "Clutch line", checked = settings.showClutch) { v ->
            onChange(settings.copy(showClutch = v))
        }
        SettingSwitch(title = "Steering line", checked = settings.showSteer) { v ->
            onChange(settings.copy(showSteer = v))
        }

        Spacer(Modifier.height(6.dp))

        SettingSwitch(title = "Header", checked = settings.showHeader) { v ->
            onChange(settings.copy(showHeader = v))
        }
        SettingSwitch(title = "Legend", checked = settings.showLegend) { v ->
            onChange(settings.copy(showLegend = v))
        }

        Spacer(Modifier.height(6.dp))

        SettingsIntSliderRow(
            title = "Width",
            tooltip = "Panel width in dp.",
            value = draft.widthDp,
            range = 260..900,
            step = 10,
            valueSuffix = "dp",
            onPreviewChange = { v -> draft = draft.copy(widthDp = v) },
            onCommit = { onChange(draft) },
        )

        SettingsIntSliderRow(
            title = "Graph height",
            tooltip = "Graph area height in dp.",
            value = draft.graphHeightDp,
            range = 60..120,
            step = 10,
            valueSuffix = "dp",
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
            tooltip =
                "How many seconds of past inputs are visible in the graph.\n" +
                    "Shorter = more responsive (sharper). Longer = smoother, longer tail.\n" +
                    "Example: 2s shows only recent changes, 5s shows a longer trace.",
        ) {
            Text(text = "History", color = SimAnalyzerTheme.material.onSurface)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            options.forEach { sec ->
                val selected = sec == valueSec
                TextButton(
                    onClick = { onSelect(sec) },
                ) {
                    Text(
                        text = "${sec}s",
                        color = if (selected) {
                            SimAnalyzerTheme.material.primary
                        } else {
                            SimAnalyzerTheme.material.onSurfaceVariant
                        },
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
