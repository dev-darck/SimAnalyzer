package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisProjectedTrackMap
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTrackCanvasState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal fun ImmutableList<SessionAnalysisFractionPointUi>.projectFractions(
    bounds: Rect,
    canvasSize: IntSize,
    padding: Float,
): ImmutableList<SessionAnalysisFractionPointUi> {
    if (isEmpty() || canvasSize.width <= 0 || canvasSize.height <= 0) return persistentListOf()

    val scale = resolveProjectionScale(bounds = bounds, canvasSize = canvasSize, padding = padding)
    val drawWidth = bounds.width * scale
    val drawHeight = bounds.height * scale
    val offsetX = (canvasSize.width.toFloat() - drawWidth) * 0.5f
    val offsetY = (canvasSize.height.toFloat() - drawHeight) * 0.5f

    return map { point ->
        point.copy(
            x = offsetX + (point.x - bounds.left) * scale,
            y = offsetY + (point.y - bounds.top) * scale,
        )
    }.toImmutableList()
}

internal fun SessionAnalysisTrackCanvasState.projectTrackMap(
    bounds: Rect,
    canvasSize: IntSize,
    padding: Float,
): SessionAnalysisProjectedTrackMap {
    val projectedCenterLine = centerLine.projectFractions(
        bounds = bounds,
        canvasSize = canvasSize,
        padding = padding,
    )
    val projectedLeftEdge = trackLeftEdge.projectFractions(
        bounds = bounds,
        canvasSize = canvasSize,
        padding = padding,
    )
    val projectedRightEdge = trackRightEdge.projectFractions(
        bounds = bounds,
        canvasSize = canvasSize,
        padding = padding,
    )
    val projectedIdealLine = idealLine.projectFractions(
        bounds = bounds,
        canvasSize = canvasSize,
        padding = padding,
    )
    val projectedSelectedTrace = selectedTrace.projectFractions(
        bounds = bounds,
        canvasSize = canvasSize,
        padding = padding,
    )
    val projectedReferenceTrace = referenceTrace.projectFractions(
        bounds = bounds,
        canvasSize = canvasSize,
        padding = padding,
    )
    val projectedInteractionTrace = interactionTrace.projectFractions(
        bounds = bounds,
        canvasSize = canvasSize,
        padding = padding,
    )

    return SessionAnalysisProjectedTrackMap(
        centerLine = projectedCenterLine,
        leftEdge = projectedLeftEdge,
        rightEdge = projectedRightEdge,
        idealLine = projectedIdealLine,
        idealLineBreakIndices = projectedIdealLine.telemetryPathBreakIndices(),
        selectedTrace = projectedSelectedTrace,
        selectedTraceBreakIndices = projectedSelectedTrace.telemetryPathBreakIndices(),
        referenceTrace = projectedReferenceTrace,
        referenceTraceBreakIndices = projectedReferenceTrace.telemetryPathBreakIndices(),
        interactionTrace = projectedInteractionTrace,
    )
}

internal fun SessionAnalysisTrackCanvasState.trackMapWorldBounds(): Rect = Rect(
    left = minX,
    top = minY,
    right = maxX,
    bottom = maxY,
).normalize()

internal fun resolveProjectionScale(bounds: Rect, canvasSize: IntSize, padding: Float): Float = minOf(
    (canvasSize.width.toFloat() - padding * 2f) / bounds.width,
    (canvasSize.height.toFloat() - padding * 2f) / bounds.height,
).takeIf { scale -> scale.isFinite() && scale > 0f } ?: 1f

internal fun resolveProjectionPadding(trackSurfaceWidthMeters: Float, bounds: Rect, canvasSize: IntSize): Float {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return 24f
    val basePadding = 24f
    val scale = resolveProjectionScale(bounds = bounds, canvasSize = canvasSize, padding = basePadding)
    val projectedTrackWidth = (trackSurfaceWidthMeters * scale)
        .takeIf { width -> width.isFinite() && width > 0f }
        ?: 14f
    return maxOf(basePadding, projectedTrackWidth * 0.72f + 16f)
}

internal fun resolveTrackSurfaceStrokePx(trackSurfaceWidthMeters: Float, bounds: Rect, canvasSize: IntSize): Float {
    val minStroke = 14f
    val maxStroke = (minOf(canvasSize.width, canvasSize.height) * 0.18f)
        .takeIf { value -> value.isFinite() && value >= minStroke }
        ?: minStroke
    val projected = trackSurfaceWidthMeters * resolveProjectionScale(
        bounds = bounds,
        canvasSize = canvasSize,
        padding = 24f,
    )
    return projected.takeIf { value -> value.isFinite() && value > 0f }?.coerceIn(minStroke, maxStroke) ?: minStroke
}

internal fun buildTrackSurfacePath(
    leftEdge: List<SessionAnalysisFractionPointUi>,
    rightEdge: List<SessionAnalysisFractionPointUi>,
): Path? {
    if (leftEdge.size < 2 || rightEdge.size < 2) return null
    return Path().apply {
        fillType = PathFillType.EvenOdd
        moveTo(leftEdge.first().x, leftEdge.first().y)
        for (index in 1 until leftEdge.size) {
            lineTo(leftEdge[index].x, leftEdge[index].y)
        }
        for (index in rightEdge.lastIndex downTo 0) {
            lineTo(rightEdge[index].x, rightEdge[index].y)
        }
        close()
    }
}

internal fun Rect.normalize(): Rect {
    val safeWidth = width.takeIf { value -> value > 0.001f } ?: 1f
    val safeHeight = height.takeIf { value -> value > 0.001f } ?: 1f
    return Rect(left = left, top = top, right = left + safeWidth, bottom = top + safeHeight)
}
