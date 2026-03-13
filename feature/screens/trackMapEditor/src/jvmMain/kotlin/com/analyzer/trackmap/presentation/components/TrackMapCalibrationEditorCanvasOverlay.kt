package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlin.math.roundToInt

internal data class TrackMapCalibrationSelectionUiState(
    val label: String,
    val badgePoint: Offset,
    val handleCenter: Offset,
    val handleColor: Color,
)

internal data class TrackMapCalibrationSelectionOverlayColors(
    val labelBackground: Color,
    val labelText: Color,
    val labelBorder: Color,
    val handleCenter: Color,
)

@Composable
internal fun BoxScope.TrackMapCalibrationEditorSelectionOverlay(
    uiState: TrackMapCalibrationSelectionUiState?,
    colors: TrackMapCalibrationSelectionOverlayColors,
    onHandleDragStart: (Offset) -> Unit,
    onHandleDrag: (Offset) -> Unit,
    onHandleDragEnd: () -> Unit,
) {
    uiState ?: return

    TrackMapSelectedMarkerBadge(
        text = uiState.label,
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset {
                IntOffset(
                    x = (uiState.badgePoint.x - 42f).roundToInt(),
                    y = (uiState.badgePoint.y + 18f).roundToInt(),
                )
            },
        backgroundColor = colors.labelBackground,
        textColor = colors.labelText,
        borderColor = colors.labelBorder,
    )

    TrackMapEditorHandle(
        center = uiState.handleCenter,
        color = uiState.handleColor,
        innerColor = colors.handleCenter,
        handleSize = 30.dp,
        onDragStart = onHandleDragStart,
        onDrag = onHandleDrag,
        onDragEnd = onHandleDragEnd,
    )
}

@Composable
private fun TrackMapSelectedMarkerBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color,
) {
    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(backgroundColor)
            .border(1.dp, borderColor, SimAnalyzerTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = textColor,
        )
    }
}

@Composable
private fun TrackMapEditorHandle(
    center: Offset,
    color: Color,
    innerColor: Color,
    handleSize: Dp,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val halfSizePx = with(LocalDensity.current) { (handleSize / 2).toPx() }
    val currentCenter by rememberUpdatedState(center)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (center.x - halfSizePx).roundToInt(),
                    y = (center.y - halfSizePx).roundToInt(),
                )
            }
            .size(handleSize)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        currentOnDragStart(
                            Offset(
                                x = currentCenter.x - halfSizePx + start.x,
                                y = currentCenter.y - halfSizePx + start.y,
                            ),
                        )
                    },
                    onDragCancel = {
                        currentOnDragEnd()
                    },
                    onDragEnd = {
                        currentOnDragEnd()
                    },
                ) { change, _ ->
                    change.consume()
                    currentOnDrag(
                        Offset(
                            x = currentCenter.x - halfSizePx + change.position.x,
                            y = currentCenter.y - halfSizePx + change.position.y,
                        ),
                    )
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(10.dp)
                .clip(CircleShape)
                .background(innerColor)
                .border(2.dp, color, CircleShape),
        )
    }
}
