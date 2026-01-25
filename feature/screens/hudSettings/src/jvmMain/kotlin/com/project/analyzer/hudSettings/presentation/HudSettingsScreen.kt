package com.project.analyzer.hudSettings.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
internal fun HudSettingsScreen() {
    val viewModel: HudSettingsViewModel = metroViewModel()
    val state by viewModel.state.collectAsState()

    val dispatch = viewModel::dispatch
    Screen(state, dispatch)
}

@Composable
private fun Screen(
    state: HudUiState = HudUiState(panels = emptyList()),
    dispatch: (HudSettingsIntent) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = SimAnalyzerTheme.material.background)
            .padding(all = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HudSettingsPanel(
            panel = state.panel,
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .fillMaxHeight()
        )

        HudMonitorPanel(
            panel = state.panel,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

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
                .widthIn(min = 280.dp, max = 360.dp)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun HudSettingsPanel(
    panel: HudPanel?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 16.sp,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        if (panel == null) {
            Text(
                text = "Select a HUD from the list to edit its settings.",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp
            )
            return
        }

        Text(text = panel.id, color = SimAnalyzerTheme.material.onSurface, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = panel.description, color = SimAnalyzerTheme.material.onSurfaceVariant, fontSize = 12.sp)

        Spacer(Modifier.height(12.dp))

        if (!panel.hasSettings) {
            Text(
                text = "This HUD has no configurable settings.",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp
            )
            return
        }

        panel.SettingsContent(Modifier.fillMaxWidth())
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
            .padding(16.dp)
    ) {
        Text(
            text = "HUDs",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 16.sp,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(panels, key = { it.id }) { panel ->
                val enabled = panel.id in visibleIds
                HudListItem(
                    id = panel.id,
                    description = panel.description,
                    enabled = enabled,
                    onClick = onClick,
                    onToggle = { onToggle(panel.id, !enabled) }
                )
            }
        }
    }
}

@Composable
private fun HudListItem(
    id: String,
    description: String,
    enabled: Boolean,
    onClick: (id: String) -> Unit = {},
    onToggle: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(12.dp))
            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.25f))
            .clickable(
                onClick = {
                    onClick(id)
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = id,
                color = SimAnalyzerTheme.material.onSurface,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SimAnalyzerTheme.material.primary,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
private fun HudMonitorPanel(
    panel: HudPanel? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = SimAnalyzerTheme.material.surface, shape = SimAnalyzerTheme.shapes.large)
            .padding(all = 16.dp)
    ) {
        Text(
            text = "Preview",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 16.sp,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(shape = RoundedCornerShape(16.dp))
                .background(SimAnalyzerTheme.material.background)
                .border(
                    width = 1.dp,
                    color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
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

            panel?.DemoContent(Modifier)
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
                    Text("Fuel HUD", modifier = modifier.size(120.dp, 60.dp))
                }
            },
            object : HudPanel {
                override val id: String = "timing"

                @Composable
                override fun DemoContent(modifier: Modifier) {
                    Text("Timing HUD", modifier = modifier.size(140.dp, 60.dp))
                }
            },
            object : HudPanel {
                override val id: String = "electronics"

                @Composable
                override fun DemoContent(modifier: Modifier) {
                    Text("Electronics HUD", modifier = modifier.size(140.dp, 60.dp))
                }
            },
        )
    }

    SimAnalyzerTheme {
        Screen(
            state = HudUiState(
                panels = demoPanels,
                visiblePanels = mapOf("fuel" to 0, "timing" to 0, "electronics" to 0),
            )
        )
    }
}
