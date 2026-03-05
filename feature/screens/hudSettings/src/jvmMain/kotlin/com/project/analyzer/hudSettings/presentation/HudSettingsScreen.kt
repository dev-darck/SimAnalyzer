package com.project.analyzer.hudSettings.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.screens.hudSettings.Res.Res
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_common_description
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_common_title
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_huds_title
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_no_configurable
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_opacity_title
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_opacity_tooltip
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_preview_electronics
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_preview_fuel
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_preview_timing
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_preview_title
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_select_hint
import com.project.analyzer.feature.screens.hudSettings.Res.hud_settings_title
import com.project.analyzer.hud.api.DefaultHudBackgroundOpacity
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.LocalHudBackgroundOpacity
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import com.project.analyzer.ui.slider.SettingsSliderRow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

@Composable
internal fun HudSettingsScreen() {
    val viewModel: HudSettingsViewModel = metroViewModel()
    val state by viewModel.state.collectAsState()

    val dispatch: (HudSettingsIntent) -> Unit = viewModel::dispatch
    Screen(state, dispatch)
}

@Composable
private fun Screen(state: HudUiState = HudUiState(panels = emptyList()), dispatch: (HudSettingsIntent) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SimAnalyzerTheme.material.background)
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HudSettingsPanel(
            panel = state.panel,
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .fillMaxHeight(),
        )

        HudMonitorPanel(
            panel = state.panel,
            hudOpacity = state.hudOpacity,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )

        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HudListPanel(
                panels = state.panels,
                visibleIds = state.visiblePanels.keys,
                onClick = {
                    dispatch(HudSettingsIntent.OnShowPanel(it))
                },
                onToggle = { id, enable ->
                    dispatch(HudSettingsIntent.TogglePanel(id, enable))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            HudCommonSettingsPanel(
                hudOpacity = state.hudOpacity,
                onHudOpacityChange = { dispatch(HudSettingsIntent.UpdateHudOpacity(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HudSettingsPanel(panel: HudPanel?, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .padding(end = 10.dp),
        ) {
            Text(
                text = stringResource(Res.string.hud_settings_title),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(12.dp))

            if (panel == null) {
                Text(
                    text = stringResource(Res.string.hud_settings_select_hint),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                )
                return
            }

            Text(
                text = panel.id,
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = panel.description,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(12.dp))

            if (!panel.hasSettings) {
                Text(
                    text = stringResource(Res.string.hud_settings_no_configurable),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                )
                return
            }

            panel.SettingsContent(Modifier.fillMaxWidth())
        }
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 6.dp),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}

@Composable
private fun HudListPanel(
    panels: List<HudPanel>,
    visibleIds: Set<String>,
    modifier: Modifier = Modifier,
    onClick: (id: String) -> Unit = {},
    onToggle: (String, Boolean) -> Unit = { _, _ -> },
) {
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.hud_settings_huds_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(12.dp))

        val listState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(panels, key = { it.id }) { panel ->
                    val enabled = panel.id in visibleIds
                    HudListItem(
                        id = panel.id,
                        description = panel.description,
                        enabled = enabled,
                        onClick = onClick,
                        onToggle = { onToggle(panel.id, !enabled) },
                    )
                }
            }
            AppVerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                adapter = rememberScrollbarAdapter(listState),
            )
        }
    }
}

@Composable
private fun HudListItem(
    id: String,
    description: String,
    enabled: Boolean,
    onClick: (id: String) -> Unit = {},
    onToggle: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .clip(shape = SimAnalyzerTheme.corners.field)
            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.25f))
            .clickable(
                onClick = {
                    onClick(id)
                },
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = id,
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SimAnalyzerTheme.material.primary,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.25f),
            ),
        )
    }
}

@Composable
private fun HudCommonSettingsPanel(
    hudOpacity: Float,
    onHudOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.hud_settings_common_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(Res.string.hud_settings_common_description),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )

        SettingsSliderRow(
            title = stringResource(Res.string.hud_settings_opacity_title),
            tooltip = stringResource(Res.string.hud_settings_opacity_tooltip),
            value = hudOpacity.coerceIn(0f, 1f),
            valueRange = 0f..1f,
            snapStep = 0.05f,
            valueText = { value -> String.format(Locale.US, "%.2f", value) },
            onPreviewChange = onHudOpacityChange,
            onCommit = {},
        )
    }
}

@Composable
private fun HudMonitorPanel(panel: HudPanel? = null, hudOpacity: Float = 1f, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = SimAnalyzerTheme.material.surface, shape = SimAnalyzerTheme.shapes.large)
            .padding(all = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.hud_settings_preview_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(shape = SimAnalyzerTheme.corners.card)
                .background(SimAnalyzerTheme.material.background)
                .border(
                    width = 1.dp,
                    color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.12f),
                    shape = SimAnalyzerTheme.corners.card,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val gridColor = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.05f)

            Canvas(Modifier.fillMaxSize()) {
                val step = 20f
                val w = size.width
                val h = size.height
                var x = 0f
                while (x <= w) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, h))
                    x += step
                }
                var y = 0f
                while (y <= h) {
                    drawLine(gridColor, Offset(0f, y), Offset(w, y))
                    y += step
                }
            }

            CompositionLocalProvider(
                LocalHudBackgroundOpacity provides hudOpacity.coerceIn(0f, 1f),
            ) {
                panel?.DemoContent(Modifier)
            }
        }
    }
}

@Preview
@Composable
private fun HudSettingsScreenPreview() {
    val demoPanels = remember {
        listOf(
            object : HudPanel {
                override val id: String = "fuel"

                @Composable
                override fun DemoContent(modifier: Modifier) {
                    Text(
                        text = stringResource(Res.string.hud_settings_preview_fuel),
                        style = SimAnalyzerTheme.typography.labelMedium,
                        modifier = modifier.size(120.dp, 60.dp),
                    )
                }
            },
            object : HudPanel {
                override val id: String = "timing"

                @Composable
                override fun DemoContent(modifier: Modifier) {
                    Text(
                        text = stringResource(Res.string.hud_settings_preview_timing),
                        style = SimAnalyzerTheme.typography.labelMedium,
                        modifier = modifier.size(140.dp, 60.dp),
                    )
                }
            },
            object : HudPanel {
                override val id: String = "electronics"

                @Composable
                override fun DemoContent(modifier: Modifier) {
                    Text(
                        text = stringResource(Res.string.hud_settings_preview_electronics),
                        style = SimAnalyzerTheme.typography.labelMedium,
                        modifier = modifier.size(140.dp, 60.dp),
                    )
                }
            },
        )
    }

    SimAnalyzerTheme {
        Screen(
            state = HudUiState(
                panels = demoPanels,
                visiblePanels = mapOf("fuel" to 0, "timing" to 0, "electronics" to 0),
                hudOpacity = DefaultHudBackgroundOpacity,
            ),
        )
    }
}
