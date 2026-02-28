package com.project.analyzer.impl.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.project.analyzer.hud.api.HudStoredPosition
import com.project.analyzer.impl.setup.game.OverlayController
import com.project.analyzer.impl.setup.region.HitRegions
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlin.math.roundToInt

@Composable
internal fun DraggableHudBox(
    panelId: String,
    offset: IntOffset,
    containerSize: IntSize,
    panelSize: IntSize?,
    hitRegions: HitRegions,
    overlayController: OverlayController,
    onIntent: (HudIntent) -> Unit,
    inputLocked: Boolean,
    onToggleInputLock: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    var localOffset by remember(panelId) { mutableStateOf(offset) }

    LaunchedEffect(offset) {
        localOffset = offset
    }

    var isDragging by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var lastRect by remember { mutableStateOf<IntRect?>(null) }

    val draggableState = rememberDraggable2DState { delta ->
        localOffset = IntOffset(
            x = localOffset.x + delta.x.roundToInt(),
            y = localOffset.y + delta.y.roundToInt(),
        )
    }

    val backdropColor by animateColorAsState(
        targetValue = if (isDragging || isHovered) {
            Color.Black.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
    )

    DisposableEffect(panelId) {
        onDispose {
            if (isDragging) {
                overlayController.endDrag()
            }
            hitRegions.remove(panelId)
        }
    }

    Box(
        modifier = modifier
            .offset { localOffset }
            .hoverable(interactionSource)
            .background(backdropColor, SimAnalyzerTheme.corners.control)
            .onGloballyPositioned { coords ->
                val p = coords.positionInRoot()
                val s = coords.size
                val rect = IntRect(
                    left = p.x.roundToInt(),
                    top = p.y.roundToInt(),
                    right = (p.x + s.width).roundToInt(),
                    bottom = (p.y + s.height).roundToInt(),
                )
                if (rect != lastRect) {
                    lastRect = rect
                    if (!isDragging) {
                        hitRegions.put(panelId, rect)
                    }
                }
            }
            .draggable2D(
                state = draggableState,
                onDragStarted = {
                    overlayController.beginDrag()
                    isDragging = true
                },
                onDragStopped = {
                    isDragging = false
                    lastRect?.let { hitRegions.put(panelId, it) }
                    overlayController.endDrag()
                    val savedPosition = panelSize?.let { size ->
                        normalizeDraggedOffset(localOffset, containerSize, size)
                    } ?: HudStoredPosition.Absolute(localOffset)
                    onIntent(HudIntent.SavePosition(panelId, savedPosition))
                },
            ),
    ) {
        content()

        if (onToggleInputLock != null && (inputLocked || isHovered || isDragging)) {
            val shape = SimAnalyzerTheme.corners.compact
            val accent = if (inputLocked) SimAnalyzerTheme.material.primary else SimAnalyzerTheme.material.onSurface
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(shape)
                    .background(SimAnalyzerTheme.chrome.fillOverlay, shape)
                    .clickable(onClick = onToggleInputLock)
                    .padding(4.dp),
            ) {
                Icon(
                    imageVector = if (inputLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun normalizeDraggedOffset(
    offset: IntOffset,
    containerSize: IntSize,
    panelSize: IntSize,
): HudStoredPosition.Normalized {
    val maxX = (containerSize.width - panelSize.width).coerceAtLeast(0)
    val maxY = (containerSize.height - panelSize.height).coerceAtLeast(0)

    val xFraction = if (maxX == 0) 0f else offset.x.toFloat() / maxX.toFloat()
    val yFraction = if (maxY == 0) 0f else offset.y.toFloat() / maxY.toFloat()

    return HudStoredPosition.Normalized(
        xFraction = xFraction.coerceIn(0f, 1f),
        yFraction = yFraction.coerceIn(0f, 1f),
    )
}
