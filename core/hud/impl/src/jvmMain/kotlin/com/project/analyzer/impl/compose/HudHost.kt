package com.project.analyzer.impl.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.project.analyzer.hud.api.HudPanel
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun HudHost(
    panels: Set<HudPanel>,
    modifier: Modifier = Modifier
) {
    val viewModel: HudViewModel = metroViewModel()
    val state by viewModel.state.collectAsState()
    val dispatch = viewModel::dispatch
    val panelsById = remember(panels) { panels.associateBy { it.id } }

    Box(modifier = modifier.fillMaxSize()) {
        state.visiblePanels
            .mapNotNull { (id, version) ->
                panelsById[id]?.let { panel -> Triple(id, version, panel) }
            }
            .sortedBy { (_, _, panel) -> panel.zIndex }
            .forEach { (id, version, panel) ->
                key(id, version) {
                    DraggableHudBox(
                        panelId = id,
                        offset = state.positions[id] ?: panel.defaultOffset,
                        onIntent = dispatch,
                        modifier = Modifier.zIndex(panel.zIndex.toFloat())
                    ) {
                        panel.Content(Modifier)
                    }
                }
            }
    }
}
