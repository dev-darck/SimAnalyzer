package com.project.analyzer.impl.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.project.analyzer.hud.api.HudAnchor
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.impl.setup.game.OverlayController
import com.project.analyzer.impl.setup.region.HitRegions
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
internal fun HudHost(
    panels: Set<HudPanel>,
    hitRegions: HitRegions,
    overlayController: OverlayController,
    modifier: Modifier = Modifier,
) {
    val viewModel: HudViewModel = metroViewModel()
    val state by viewModel.state.collectAsState()
    val dispatch = viewModel::dispatch
    val panelsById = remember(panels) { panels.associateBy { it.id } }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val panelSizes = remember { mutableStateMapOf<String, IntSize>() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        state.visiblePanels
            .mapNotNull { (id, version) ->
                panelsById[id]?.let { panel -> Triple(id, version, panel) }
            }
            .sortedBy { (_, _, panel) -> panel.zIndex }
            .forEach { (id, version, panel) ->
                key(id, version) {
                    DraggableHudBox(
                        panelId = id,
                        offset = state.positions[id] ?: computeDefaultOffset(
                            anchor = panel.defaultAnchor,
                            margin = panel.defaultMarginPx,
                            container = containerSize,
                            panel = panelSizes.get(id)
                        ),
                        onIntent = dispatch,
                        hitRegions = hitRegions,
                        overlayController = overlayController,
                        modifier = Modifier.zIndex(panel.zIndex.toFloat())
                    ) {
                        panel.Content(
                            modifier = Modifier
                                .padding(6.dp)
                                .onSizeChanged { panelSizes[id] = it }
                        )
                    }
                }
            }
    }
}

private fun computeDefaultOffset(
    anchor: HudAnchor,
    margin: IntOffset,
    container: IntSize,
    panel: IntSize?
): IntOffset {
    if (panel == null || container.width <= 0 || container.height <= 0) return IntOffset.Zero

    val raw = when (anchor) {
        HudAnchor.TopLeft -> IntOffset(margin.x, margin.y)
        HudAnchor.TopRight -> IntOffset(container.width - panel.width - margin.x, margin.y)
        HudAnchor.BottomLeft -> IntOffset(margin.x, container.height - panel.height - margin.y)
        HudAnchor.BottomRight -> IntOffset(
            container.width - panel.width - margin.x,
            container.height - panel.height - margin.y
        )

        HudAnchor.Center -> IntOffset(
            (container.width - panel.width) / 2,
            (container.height - panel.height) / 2
        )
    }

    val maxX = (container.width - panel.width).coerceAtLeast(0)
    val maxY = (container.height - panel.height).coerceAtLeast(0)

    return IntOffset(
        x = raw.x.coerceIn(0, maxX),
        y = raw.y.coerceIn(0, maxY)
    )
}
