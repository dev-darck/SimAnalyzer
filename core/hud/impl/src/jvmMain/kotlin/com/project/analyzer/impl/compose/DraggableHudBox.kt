package com.project.analyzer.impl.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
internal fun DraggableHudBox(
    panelId: String,
    offset: IntOffset,
    onIntent: (HudIntent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .offset { offset }
            .pointerInput(panelId) {
                detectDragGestures(
                    onDragEnd = {
                        onIntent(HudIntent.SavePosition(panelId))
                    },
                    onDragCancel = {
                        onIntent(HudIntent.SavePosition(panelId))
                    },
                    onDrag = { change, dragAmount ->
                        if (change.positionChange() != Offset.Zero) change.consume()
                        val newOffset = IntOffset(
                            x = offset.x + dragAmount.x.roundToInt(),
                            y = offset.y + dragAmount.y.roundToInt()
                        )
                        onIntent(HudIntent.UpdatePosition(panelId, newOffset))
                    }
                )
            },
        content = content
    )
}
