package com.analyzer.trackmap.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlin.math.min

@Composable
fun TrackMapPreview(
    state: TrackMapPreviewUi,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 1f,
    borderAlpha: Float = 0.4f,
    showStatus: Boolean = false,
) {
    val background = SimAnalyzerTheme.material.surface.copy(alpha = backgroundAlpha)
    val borderColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = borderAlpha)

    Box(
        modifier = modifier
            .background(background, SimAnalyzerTheme.shapes.large)
            .border(1.dp, borderColor, SimAnalyzerTheme.shapes.large)
            .padding(12.dp),
    ) {
        if (state.points.size < 2) {
            Text(
                text = "Start recording to build the map",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            TrackMapPreviewCanvas(
                state = state,
                modifier = Modifier.matchParentSize(),
            )
        }

        if (showStatus) {
            TrackMapPreviewStatus(
                state = state,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
private fun TrackMapPreviewStatus(state: TrackMapPreviewUi, modifier: Modifier = Modifier) {
    val statusColor = if (state.recording) SimAnalyzerTheme.extended.teal else SimAnalyzerTheme.extended.amber
    val background = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .background(background, SimAnalyzerTheme.shapes.medium)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(
            text = if (state.recording) "Recording" else "Idle",
            color = statusColor,
            style = SimAnalyzerTheme.typography.labelMedium,
        )
        Text(
            text = "Points: ${state.pointCount}",
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
        Text(
            text = "Distance: %.1f m".format(state.totalDistanceMeters),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
        Text(
            text = "Width: %.1f m".format(state.averageTrackWidthMeters),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
        state.guidanceText?.let { hint ->
            Text(
                text = hint,
                color = SimAnalyzerTheme.extended.amber,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun TrackMapPreviewCanvas(state: TrackMapPreviewUi, modifier: Modifier = Modifier) {
    val points = state.points
    if (points.size < 2) return

    val lineColor = SimAnalyzerTheme.extended.cyan
    val highlightColor = SimAnalyzerTheme.extended.purple
    val highlightGlow = SimAnalyzerTheme.extended.pink
    val markerColor = SimAnalyzerTheme.extended.lightPink
    val currentColor = SimAnalyzerTheme.extended.teal
    val corridorColor = SimAnalyzerTheme.material.primary
    val boundaryColor = SimAnalyzerTheme.material.primary.copy(alpha = 0.55f)
    val pitEntryColor = SimAnalyzerTheme.extended.amber
    val pitExitColor = SimAnalyzerTheme.extended.lightGreen
    val pitLineColor = SimAnalyzerTheme.extended.amber
    val sectorMarkerColor = SimAnalyzerTheme.extended.amber

    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    val baseStroke = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dashEffect)
    val glowStroke = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dashEffect)

    Canvas(modifier = modifier) {
        val bounds = state.bounds ?: computeBounds(points) ?: return@Canvas
        val widthMeters = (bounds.maxX - bounds.minX).coerceAtLeast(1f)
        val heightMeters = (bounds.maxY - bounds.minY).coerceAtLeast(1f)
        val padPx = 16f
        val scale = min((size.width - padPx * 2f) / widthMeters, (size.height - padPx * 2f) / heightMeters)
        val offsetX = (size.width - widthMeters * scale) * 0.5f - bounds.minX * scale
        val offsetY = (size.height - heightMeters * scale) * 0.5f - bounds.minY * scale

        fun toScreen(point: Vec2): Offset = Offset(
            x = point.x * scale + offsetX,
            y = point.y * scale + offsetY,
        )

        val path = Path()
        points.forEachIndexed { index, point ->
            val mapped = toScreen(point)
            if (index == 0) path.moveTo(mapped.x, mapped.y) else path.lineTo(mapped.x, mapped.y)
        }

        val hasWidths = state.leftWidthsMeters.size == points.size && state.rightWidthsMeters.size == points.size
        if (hasWidths && points.size >= 3) {
            val leftWorld = ArrayList<Vec2>(points.size)
            val rightWorld = ArrayList<Vec2>(points.size)
            points.indices.forEach { index ->
                val normal = localNormal(points, index)
                leftWorld += points[index] + normal * state.leftWidthsMeters[index].coerceAtLeast(0f)
                rightWorld += points[index] - normal * state.rightWidthsMeters[index].coerceAtLeast(0f)
            }
            val corridorPath = Path()
            leftWorld.forEachIndexed { index, point ->
                val mapped = toScreen(point)
                if (index == 0) corridorPath.moveTo(mapped.x, mapped.y) else corridorPath.lineTo(mapped.x, mapped.y)
            }
            for (index in rightWorld.lastIndex downTo 0) {
                val mapped = toScreen(rightWorld[index])
                corridorPath.lineTo(mapped.x, mapped.y)
            }
            corridorPath.close()
            drawPath(path = corridorPath, color = corridorColor.copy(alpha = 0.08f))
            drawPath(
                path = buildPath(leftWorld, ::toScreen),
                color = boundaryColor,
                style = Stroke(width = 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = buildPath(rightWorld, ::toScreen),
                color = boundaryColor,
                style = Stroke(width = 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        drawPath(path = path, color = lineColor.copy(alpha = 0.35f), style = glowStroke)
        drawPath(path = path, color = lineColor, style = baseStroke)

        if (state.pitPoints.size >= 2) {
            val pitPath = buildPath(state.pitPoints, ::toScreen)
            drawPath(
                path = pitPath,
                color = pitLineColor.copy(alpha = 0.45f),
                style = Stroke(
                    width = 4f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                ),
            )
            drawPath(
                path = pitPath,
                color = pitLineColor,
                style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        val highlightPoints = if (state.recording) points.takeLast(min(points.size, 60)) else emptyList()
        if (highlightPoints.size >= 2) {
            val highlightPath = Path()
            highlightPoints.forEachIndexed { index, point ->
                val mapped = toScreen(point)
                if (index == 0) highlightPath.moveTo(mapped.x, mapped.y) else highlightPath.lineTo(mapped.x, mapped.y)
            }
            drawPath(
                path = highlightPath,
                color = highlightGlow.copy(alpha = 0.6f),
                style = Stroke(
                    width = 10f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            drawPath(
                path = highlightPath,
                color = highlightColor,
                style = Stroke(
                    width = 6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            drawCircle(color = markerColor, radius = 6f, center = toScreen(highlightPoints.first()))
            drawCircle(color = markerColor, radius = 6f, center = toScreen(highlightPoints.last()))
        }

        fun drawPitMarker(point: Vec2, color: androidx.compose.ui.graphics.Color) {
            val mapped = toScreen(point)
            drawCircle(color = color.copy(alpha = 0.28f), radius = 10f, center = mapped)
            drawCircle(color = color, radius = 5.5f, center = mapped)
        }

        state.pitEntryPoint?.takeIf { it.x.isFinite() && it.y.isFinite() }?.let { drawPitMarker(it, pitEntryColor) }
        state.pitExitPoint?.takeIf { it.x.isFinite() && it.y.isFinite() }?.let { drawPitMarker(it, pitExitColor) }
        state.sectorMarkerPositions.forEach { point ->
            point.takeIf { it.x.isFinite() && it.y.isFinite() }?.let {
                val mapped = toScreen(it)
                drawCircle(color = sectorMarkerColor.copy(alpha = 0.24f), radius = 9f, center = mapped)
                drawCircle(color = sectorMarkerColor, radius = 4.5f, center = mapped)
            }
        }
        state.currentPosition?.let { point ->
            drawCircle(color = currentColor, radius = 4f, center = toScreen(point))
        }
    }
}

private fun localNormal(points: List<Vec2>, index: Int): Vec2 {
    val count = points.size
    if (count < 2) return Vec2.Up
    val prev = points[(index - 1 + count) % count]
    val next = points[(index + 1) % count]
    return (next - prev).safeNormalized(Vec2.Up).perpLeft().safeNormalized(Vec2.Up)
}

private fun buildPath(points: List<Vec2>, mapper: (Vec2) -> Offset): Path {
    val path = Path()
    points.forEachIndexed { index, point ->
        val mapped = mapper(point)
        if (index == 0) path.moveTo(mapped.x, mapped.y) else path.lineTo(mapped.x, mapped.y)
    }
    return path
}

private fun computeBounds(points: List<Vec2>): TrackMapBounds? {
    if (points.isEmpty()) return null
    var minX = points[0].x
    var minY = points[0].y
    var maxX = points[0].x
    var maxY = points[0].y
    points.forEach { point ->
        if (point.x < minX) minX = point.x
        if (point.y < minY) minY = point.y
        if (point.x > maxX) maxX = point.x
        if (point.y > maxY) maxY = point.y
    }
    return TrackMapBounds(minX = minX, minY = minY, maxX = maxX, maxY = maxY)
}
