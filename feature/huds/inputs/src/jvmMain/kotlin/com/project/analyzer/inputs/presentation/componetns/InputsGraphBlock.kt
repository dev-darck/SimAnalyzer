@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.inputs.presentation.componetns

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.huds.inputs.Res.Res
import com.project.analyzer.feature.huds.inputs.Res.inputs_graph_axis_0
import com.project.analyzer.feature.huds.inputs.Res.inputs_graph_axis_100
import com.project.analyzer.feature.huds.inputs.Res.inputs_graph_axis_50
import com.project.analyzer.inputs.presentation.InputsHudUiState
import com.project.analyzer.inputs.presentation.model.InputsSeries
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))

@Composable
internal fun InputsGraphBlock(state: InputsHudUiState, modifier: Modifier = Modifier) {
    val throttlePath = remember { Path() }
    val brakePath = remember { Path() }
    val clutchPath = remember { Path() }
    val steerPath = remember { Path() }

    Row(modifier = modifier) {
        if (state.settings.showLegend) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = stringResource(Res.string.inputs_graph_axis_100),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelSmall,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.inputs_graph_axis_50),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelSmall,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.inputs_graph_axis_0),
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelSmall,
                )
            }

            Spacer(Modifier.width(8.dp))
        }

        val gridColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.35f)
        val steerColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.75f)
        val tColor = SimAnalyzerTheme.extended.lightGreen
        val bColor = SimAnalyzerTheme.extended.red
        val cColor = SimAnalyzerTheme.extended.amber
        val sColor = SimAnalyzerTheme.extended.cyan

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // space
            val pad = 4.dp.toPx()

            // area of graphics
            val left = 0f + pad
            val right = w - pad
            val top = 0f + pad
            val bottom = h - pad

            val plotW = (right - left).coerceAtLeast(1f)
            val plotH = (bottom - top).coerceAtLeast(1f)
            val midY = top + plotH * 0.5f

            fun yPedal(v: Float): Float = bottom - v.coerceIn(0f, 1f) * plotH
            for (k in 0..4) {
                val v = k / 4f
                val y = yPedal(v)
                drawLine(
                    color = gridColor,
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }

            drawLine(
                color = steerColor,
                start = Offset(left, midY),
                end = Offset(right, midY),
                strokeWidth = 1.dp.toPx(),
            )

            for (k in 0..6) {
                val x = left + plotW * (k / 6f)
                drawLine(
                    color = gridColor,
                    start = Offset(x, top),
                    end = Offset(x, bottom),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }

            if (state.settings.showThrottle) {
                drawSeries(
                    series = state.series,
                    left = left,
                    plotW = plotW,
                    plotH = plotH,
                    bottom = bottom,
                    midY = midY,
                    channel = InputsGraphChannel.Throttle,
                    color = tColor,
                    path = throttlePath,
                )
            }
            if (state.settings.showBrake) {
                drawSeries(
                    series = state.series,
                    left = left,
                    plotW = plotW,
                    plotH = plotH,
                    bottom = bottom,
                    midY = midY,
                    channel = InputsGraphChannel.Brake,
                    color = bColor,
                    path = brakePath,
                )
            }
            if (state.settings.showClutch) {
                drawSeries(
                    series = state.series,
                    left = left,
                    plotW = plotW,
                    plotH = plotH,
                    bottom = bottom,
                    midY = midY,
                    channel = InputsGraphChannel.Clutch,
                    color = cColor,
                    path = clutchPath,
                )
            }
            if (state.settings.showSteer) {
                drawSeries(
                    series = state.series,
                    left = left,
                    plotW = plotW,
                    plotH = plotH,
                    bottom = bottom,
                    midY = midY,
                    channel = InputsGraphChannel.Steer,
                    color = sColor,
                    path = steerPath,
                )
            }
        }
    }
}

private fun DrawScope.drawSeries(
    series: InputsSeries,
    left: Float,
    plotW: Float,
    plotH: Float,
    bottom: Float,
    midY: Float,
    channel: InputsGraphChannel,
    color: Color,
    path: Path,
) {
    val n = series.size
    if (n < 2) return

    val dx = plotW / (n - 1).toFloat()
    val maxPoints = plotW.roundToInt().coerceAtLeast(2)
    val step = ((n - 1) + (maxPoints - 2)) / (maxPoints - 1)
    path.reset()
    val halfPlotH = plotH * 0.5f

    var first = true
    series.forEachOldestToNewest(step = step) { i, t, b, c, s ->
        val x = left + i * dx
        val v = when (channel) {
            InputsGraphChannel.Throttle -> t
            InputsGraphChannel.Brake -> b
            InputsGraphChannel.Clutch -> c
            InputsGraphChannel.Steer -> s
        }
        val y = if (channel == InputsGraphChannel.Steer) {
            midY - v.coerceIn(-1f, 1f) * halfPlotH
        } else {
            bottom - v.coerceIn(0f, 1f) * plotH
        }
        if (first) {
            path.moveTo(x, y)
            first = false
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private enum class InputsGraphChannel {
    Throttle,
    Brake,
    Clutch,
    Steer,
}
