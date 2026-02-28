package com.project.analyzer.impl.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.hud.api.HudAnchor
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.LocalHudBackgroundOpacity
import com.project.analyzer.hud.api.HudStoredPosition
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dispatch: (HudIntent) -> Unit = viewModel::dispatch
    val panelsById = remember(panels) { panels.associateBy { it.id } }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val panelSizes = remember { mutableStateMapOf<String, IntSize>() }
    val perfProfiler = rememberHudRenderProfiler()

    LaunchedEffect(state.inputLocked) {
        overlayController.setInputLocked(state.inputLocked)
    }

    SideEffect {
        perfProfiler?.onRecomposition(visiblePanelsCount = state.visiblePanels.size)
    }

    HudRenderFrameProfilerEffect(perfProfiler)

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (perfProfiler == null) {
                    Modifier
                } else {
                    Modifier.drawWithContent {
                        val startedNs = System.nanoTime()
                        drawContent()
                        perfProfiler.onDrawCompleted(System.nanoTime() - startedNs)
                    }
                },
            )
            .onSizeChanged { containerSize = it },
    ) {
        state.visiblePanels
            .mapNotNull { (id, version) ->
                panelsById[id]?.let { panel -> Triple(id, version, panel) }
            }
            .sortedBy { (_, _, panel) -> panel.zIndex }
            .forEach { (id, version, panel) ->
                key(id, version) {
                    val panelSize = panelSizes[id]
                    val storedPosition = state.positions[id]
                    val defaultOffset = computeDefaultOffset(
                        anchor = panel.defaultAnchor,
                        margin = panel.defaultMarginPx,
                        container = containerSize,
                        panel = panelSize,
                    )
                    val resolvedOffset = resolveHudOffset(
                        storedPosition = storedPosition,
                        defaultOffset = defaultOffset,
                        container = containerSize,
                        panel = panelSize,
                    )

                    LaunchedEffect(id, storedPosition, containerSize, panelSize) {
                        if (storedPosition is HudStoredPosition.Absolute && panelSize != null) {
                            dispatch(
                                HudIntent.SavePosition(
                                    id = id,
                                    position = normalizeHudOffset(
                                        offset = storedPosition.offset,
                                        container = containerSize,
                                        panel = panelSize,
                                    ),
                                ),
                            )
                        }
                    }

                    DraggableHudBox(
                        panelId = id,
                        offset = resolvedOffset,
                        containerSize = containerSize,
                        panelSize = panelSize,
                        onIntent = dispatch,
                        inputLocked = state.inputLocked,
                        onToggleInputLock = { dispatch(HudIntent.ToggleInputLock) },
                        hitRegions = hitRegions,
                        overlayController = overlayController,
                        modifier = Modifier.zIndex(panel.zIndex.toFloat()),
                    ) {
                        CompositionLocalProvider(
                            LocalHudBackgroundOpacity provides state.hudOpacity.coerceIn(0f, 1f),
                        ) {
                            panel.Content(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .onSizeChanged { panelSizes[id] = it },
                            )
                        }
                    }
                }
            }
    }
}

private fun computeDefaultOffset(
    anchor: HudAnchor,
    margin: IntOffset,
    container: IntSize,
    panel: IntSize?,
): IntOffset {
    if (panel == null || container.width <= 0 || container.height <= 0) return IntOffset.Zero

    val raw = when (anchor) {
        HudAnchor.TopLeft -> IntOffset(margin.x, margin.y)

        HudAnchor.TopRight -> IntOffset(container.width - panel.width - margin.x, margin.y)

        HudAnchor.BottomLeft -> IntOffset(margin.x, container.height - panel.height - margin.y)

        HudAnchor.BottomRight -> IntOffset(
            container.width - panel.width - margin.x,
            container.height - panel.height - margin.y,
        )

        HudAnchor.Center -> IntOffset(
            (container.width - panel.width) / 2,
            (container.height - panel.height) / 2,
        )
    }

    val maxX = (container.width - panel.width).coerceAtLeast(0)
    val maxY = (container.height - panel.height).coerceAtLeast(0)

    return IntOffset(
        x = raw.x.coerceIn(0, maxX),
        y = raw.y.coerceIn(0, maxY),
    )
}

private fun resolveHudOffset(
    storedPosition: HudStoredPosition?,
    defaultOffset: IntOffset,
    container: IntSize,
    panel: IntSize?,
): IntOffset {
    if (panel == null || container.width <= 0 || container.height <= 0) return defaultOffset

    val maxX = (container.width - panel.width).coerceAtLeast(0)
    val maxY = (container.height - panel.height).coerceAtLeast(0)

    return when (storedPosition) {
        null -> defaultOffset
        is HudStoredPosition.Absolute -> IntOffset(
            x = storedPosition.offset.x.coerceIn(0, maxX),
            y = storedPosition.offset.y.coerceIn(0, maxY),
        )

        is HudStoredPosition.Normalized -> IntOffset(
            x = (storedPosition.xFraction.coerceIn(0f, 1f) * maxX).toInt(),
            y = (storedPosition.yFraction.coerceIn(0f, 1f) * maxY).toInt(),
        )
    }
}

private fun normalizeHudOffset(
    offset: IntOffset,
    container: IntSize,
    panel: IntSize,
): HudStoredPosition.Normalized {
    val maxX = (container.width - panel.width).coerceAtLeast(0)
    val maxY = (container.height - panel.height).coerceAtLeast(0)

    val xFraction = if (maxX == 0) 0f else offset.x.toFloat() / maxX.toFloat()
    val yFraction = if (maxY == 0) 0f else offset.y.toFloat() / maxY.toFloat()

    return HudStoredPosition.Normalized(
        xFraction = xFraction.coerceIn(0f, 1f),
        yFraction = yFraction.coerceIn(0f, 1f),
    )
}
