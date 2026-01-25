@file:OptIn(ExperimentalFoundationApi::class)

package com.project.analyzer.ui.slider

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.tooltip.Tooltip
import kotlin.math.roundToInt

private val TRACK_HEIGHT: Dp = 8.dp
private val THUMB_RING: Dp = 6.dp
public val THUMB_RADIUS: Dp = 18.dp

@Composable
public fun SettingsIntSliderRow(
    title: String,
    tooltip: String? = null,
    value: Int,
    range: IntRange,
    step: Int = 1,
    valueSuffix: String = "",
    onPreviewChange: (Int) -> Unit = {},
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SettingsSliderRow(
        title = title,
        tooltip = tooltip,
        value = value.toFloat(),
        valueRange = range.first.toFloat()..range.last.toFloat(),
        snapStep = step.toFloat(),
        valueText = { v ->
            val intV = v.roundToInt()
            if (valueSuffix.isNotEmpty()) "$intV $valueSuffix" else intV.toString()
        },
        onPreviewChange = { v ->
            onPreviewChange(v.roundToInt().coerceIn(range.first, range.last))
        },
        onCommit = onCommit,
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
public fun SettingsSliderRow(
    title: String,
    tooltip: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    snapStep: Float = 1f,
    valueText: (Float) -> String = { it.toString() },
    onPreviewChange: (Float) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (tooltip != null) {
                Tooltip(tooltip = tooltip) {
                    Text(text = title, color = SimAnalyzerTheme.material.onSurface, modifier = Modifier.weight(1f))
                }
            } else {
                Text(text = title, color = SimAnalyzerTheme.material.onSurface, modifier = Modifier.weight(1f))
            }

            Text(
                text = valueText(value),
                color = SimAnalyzerTheme.material.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(6.dp))

        CustomSlider(
            value = value,
            onValueChange = onPreviewChange,
            onValueChangeFinished = onCommit,
            valueRange = valueRange,
            snapStep = snapStep,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
public fun CustomSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    onValueChange: (Float) -> Unit = {},
    snapStep: Float = 1f,
    enabled: Boolean = true,
    trackAlpha: Float = 0.10f,
    thumbRingAlpha: Float = 0.35f,
) {
    val density = LocalDensity.current

    val thumbRadiusPx = with(density) { THUMB_RADIUS.toPx() }
    val ringPx = with(density) { THUMB_RING.toPx() }
    val trackHeightPx = with(density) { TRACK_HEIGHT.toPx() }

    val trackColor = SimAnalyzerTheme.material.onSurface.copy(alpha = trackAlpha)
    val thumbColor = SimAnalyzerTheme.material.primary
    val thumbRingColor = SimAnalyzerTheme.material.primary.copy(alpha = thumbRingAlpha)

    val onValueChangeState = rememberUpdatedState(onValueChange)
    val onFinishedState = rememberUpdatedState(onValueChangeFinished)

    fun valueFromX(xPx: Float, widthPx: Float): Float {
        val right = widthPx - thumbRadiusPx
        val trackWidth = (right - thumbRadiusPx).coerceAtLeast(1f)

        val xClamped = xPx.coerceIn(thumbRadiusPx, right)
        val fraction = (xClamped - thumbRadiusPx) / trackWidth

        val start = valueRange.start
        val end = valueRange.endInclusive
        val raw = start + fraction * (end - start)

        val snapped = if (snapStep > 0f) {
            (raw / snapStep).roundToInt() * snapStep
        } else raw

        return snapped.coerceIn(start, end)
    }

    Box(
        modifier = modifier
            .height(THUMB_RADIUS * 2)
            .then(
                if (enabled) {
                    Modifier.pointerInput(valueRange.start, valueRange.endInclusive, thumbRadiusPx, snapStep) {
                        awaitEachGesture {
                            val down = awaitFirstDown()

                            onValueChangeState.value(valueFromX(down.position.x, size.width.toFloat()))

                            drag(down.id) { change ->
                                change.consume()
                                onValueChangeState.value(valueFromX(change.position.x, size.width.toFloat()))
                            }

                            onFinishedState.value()
                        }
                    }
                } else Modifier
            )
            .drawWithCache {
                val start = valueRange.start
                val end = valueRange.endInclusive
                val span = (end - start).takeIf { it != 0f } ?: 1f

                val trackWidth = size.width - thumbRadiusPx * 2f
                val fraction = ((value - start) / span).coerceIn(0f, 1f)
                val thumbCx = thumbRadiusPx + fraction * trackWidth
                val cy = size.height / 2f

                onDrawBehind {
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(thumbRadiusPx, cy - trackHeightPx / 2f),
                        size = Size(trackWidth, trackHeightPx),
                        cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                    )

                    drawCircle(
                        color = thumbRingColor,
                        radius = thumbRadiusPx,
                        center = Offset(thumbCx, cy)
                    )

                    drawCircle(
                        color = thumbColor,
                        radius = (thumbRadiusPx - ringPx).coerceAtLeast(1f),
                        center = Offset(thumbCx, cy)
                    )
                }
            }
    )
}
