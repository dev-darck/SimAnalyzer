package com.project.analyzer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.max
import kotlin.math.min

public data class TrackMapPoint(val x: Float, val y: Float)

public data class TrackMapBounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

public data class TrackMapData(
    val points: ImmutableList<TrackMapPoint>,
    val pitPoints: ImmutableList<TrackMapPoint> = persistentListOf(),
    val bounds: TrackMapBounds,
)

@Composable
public fun TrackMap(
    trackMap: TrackMapData?,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    lineColor: Color = SimAnalyzerTheme.material.primary,
    pitLineColor: Color = SimAnalyzerTheme.extended.teal,
    padding: Dp = 2.dp,
    strokeWidth: Float = 1.8f,
    pitStrokeWidth: Float = 1.3f,
) {
    val map = trackMap ?: return
    val drawScaleMultiplier = max(scale, 0.1f)

    Canvas(modifier = modifier) {
        val mapWidth = map.bounds.maxX - map.bounds.minX
        val mapHeight = map.bounds.maxY - map.bounds.minY
        if (mapWidth <= 0f || mapHeight <= 0f) return@Canvas

        val padPx = padding.toPx().coerceAtLeast(0f)
        val fitScale = min(
            (size.width - padPx * 2f) / mapWidth,
            (size.height - padPx * 2f) / mapHeight,
        )
        if (!fitScale.isFinite() || fitScale <= 0f) return@Canvas

        val drawScale = fitScale * drawScaleMultiplier
        val centerX = (map.bounds.minX + map.bounds.maxX) * 0.5f
        val centerY = (map.bounds.minY + map.bounds.maxY) * 0.5f
        val offsetX = size.width * 0.5f - centerX * drawScale
        val offsetY = size.height * 0.5f - centerY * drawScale

        fun toScreen(point: TrackMapPoint): Offset = Offset(
            x = point.x * drawScale + offsetX,
            y = point.y * drawScale + offsetY,
        )

        if (map.pitPoints.size >= 2) {
            drawPath(
                path = buildPath(map.pitPoints, ::toScreen),
                color = pitLineColor,
                style = Stroke(
                    width = pitStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        drawPath(
            path = buildPath(map.points, ::toScreen),
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private fun buildPath(points: ImmutableList<TrackMapPoint>, toScreen: (TrackMapPoint) -> Offset): Path {
    val path = Path()
    points.forEachIndexed { index, point ->
        val mapped = toScreen(point)
        if (index == 0) {
            path.moveTo(mapped.x, mapped.y)
        } else {
            path.lineTo(mapped.x, mapped.y)
        }
    }
    return path
}
