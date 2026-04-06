package com.project.analyzer.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.geometry.buildSegmentedTrackPath
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

public data class TrackMapPoint(
    val x: Float,
    val y: Float,
    val leftWidthMeters: Float = 0f,
    val rightWidthMeters: Float = 0f,
)

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
    padding: Dp = 2.dp,
    strokeWidth: Float = 1.8f,
) {
    val map = trackMap ?: return
    val drawScaleMultiplier = max(scale, 0.1f)

    Spacer(
        modifier = modifier.drawWithCache {
            val emptyDraw = onDrawBehind {}
            val mainPoints = map.points.trimLoopClosureDuplicate()
            val mapWidth = map.bounds.maxX - map.bounds.minX
            val mapHeight = map.bounds.maxY - map.bounds.minY
            if (mapWidth <= 0f || mapHeight <= 0f) {
                return@drawWithCache emptyDraw
            }

            val padPx = padding.toPx().coerceAtLeast(0f)
            val fitScale = min(
                (size.width - padPx * 2f) / mapWidth,
                (size.height - padPx * 2f) / mapHeight,
            )
            if (!fitScale.isFinite() || fitScale <= 0f) {
                return@drawWithCache emptyDraw
            }

            val drawScale = fitScale * drawScaleMultiplier
            val centerX = (map.bounds.minX + map.bounds.maxX) * 0.5f
            val centerY = (map.bounds.minY + map.bounds.maxY) * 0.5f
            val offsetX = size.width * 0.5f - centerX * drawScale
            val offsetY = size.height * 0.5f - centerY * drawScale

            fun toScreen(point: TrackMapPoint): Offset = Offset(
                x = point.x * drawScale + offsetX,
                y = point.y * drawScale + offsetY,
            )

            val minimumGapPx = maxOf(strokeWidth * 6f, 8f)
            val mainOffsets = mainPoints.map(::toScreen)
            val mainPath = buildSegmentedTrackPath(
                points = mainOffsets,
                gapMultiplier = trackMapGapMultiplier,
                minimumGapPx = minimumGapPx,
                closeLoop = true,
            )

            onDrawBehind {
                drawPath(
                    path = mainPath,
                    color = lineColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        },
    )
}

private const val trackMapGapMultiplier = 8f

private fun List<TrackMapPoint>.trimLoopClosureDuplicate(): List<TrackMapPoint> {
    if (size < 4) return this
    val segmentLengths = buildList {
        for (index in 1 until size) {
            val previous = this@trimLoopClosureDuplicate[index - 1]
            val current = this@trimLoopClosureDuplicate[index]
            val length = hypot(current.x - previous.x, current.y - previous.y)
            if (length.isFinite() && length > 0.0001f) add(length)
        }
    }
    val medianSegmentLength = segmentLengths.medianOrNull() ?: return this
    val closureDistance = hypot(last().x - first().x, last().y - first().y)
    return if (closureDistance <= maxOf(1f, medianSegmentLength * 1.5f)) {
        dropLast(1)
    } else {
        this
    }
}

private fun List<Float>.medianOrNull(): Float? {
    if (isEmpty()) return null
    val sorted = sorted()
    val middleIndex = sorted.lastIndex / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middleIndex] + sorted[middleIndex + 1]) * 0.5f
    } else {
        sorted[middleIndex]
    }
}
