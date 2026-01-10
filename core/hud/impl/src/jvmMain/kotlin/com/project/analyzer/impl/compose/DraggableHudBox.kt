package com.project.analyzer.impl.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import com.project.analyzer.impl.setup.game.OverlayController
import com.project.analyzer.impl.setup.region.HitRegions
import kotlin.math.roundToInt

@Composable
internal fun DraggableHudBox(
    panelId: String,
    offset: IntOffset,
    hitRegions: HitRegions,
    overlayController: OverlayController,
    onIntent: (HudIntent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    var localOffset by remember(panelId) { mutableStateOf(offset) }

    LaunchedEffect(offset) {
        localOffset = offset
    }

    var isDragging by remember { mutableStateOf(false) }
    var lastRect by remember { mutableStateOf<IntRect?>(null) }

    DisposableEffect(panelId) {
        onDispose {
            hitRegions.remove(panelId)
        }
    }

    Box(
        modifier = modifier
            .offset { localOffset }
            .onGloballyPositioned { coords ->
                val p = coords.positionInRoot()
                val s = coords.size
                val rect = IntRect(
                    left = p.x.toInt(),
                    top = p.y.toInt(),
                    right = (p.x + s.width).toInt(),
                    bottom = (p.y + s.height).toInt()
                )
                lastRect = rect

                if (!isDragging) {
                    hitRegions.put(panelId, rect)
                }
            }
            .pointerInput(panelId) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        overlayController.beginDrag()
                    },
                    onDragEnd = {
                        isDragging = false
                        lastRect?.let { hitRegions.put(panelId, it) }
                        overlayController.endDrag()
                        onIntent(HudIntent.SavePosition(panelId, localOffset))
                    },
                    onDragCancel = {
                        isDragging = false
                        lastRect?.let { hitRegions.put(panelId, it) }
                        overlayController.endDrag()
                        onIntent(HudIntent.SavePosition(panelId, localOffset))
                    },
                    onDrag = { change, dragAmount ->
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                        }
                        localOffset = IntOffset(
                            x = localOffset.x + dragAmount.x.roundToInt(),
                            y = localOffset.y + dragAmount.y.roundToInt()
                        )
                    }
                )
            },
        content = content
    )
}
