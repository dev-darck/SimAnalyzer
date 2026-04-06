@file:Suppress("CyclomaticComplexMethod", "LongParameterList")

package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorBoundary
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorMarker
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val sectorBoundaryOvershootRatio: Float = 0.08f
private const val sectorBoundaryMinimumOvershootPx: Float = 2f
private const val sectorBoundaryMaximumOvershootPx: Float = 6f
private const val sectorMarkerCanvasMarginPx: Float = 14f
private const val sectorBoundaryVisibleTrackWidthRatio: Float = 1.35f
private const val sectorBoundaryMinViewportRatio: Float = 0.10f
private const val sectorBoundaryMaxViewportRatio: Float = 0.24f

internal fun buildSectorBoundaries(
    sectors: ImmutableList<SessionAnalysisSectorUi>,
    centerLine: ImmutableList<SessionAnalysisFractionPointUi>,
    leftEdge: ImmutableList<SessionAnalysisFractionPointUi>,
    rightEdge: ImmutableList<SessionAnalysisFractionPointUi>,
    fallbackHalfWidthPx: Float,
    bounds: Rect,
    canvasSize: IntSize,
    padding: Float,
): ImmutableList<SessionAnalysisTrackSectorBoundary> {
    if (sectors.isEmpty()) return persistentListOf()
    val projectionScale = resolveProjectionScale(bounds = bounds, canvasSize = canvasSize, padding = padding)
    return sectors.mapNotNull { sector ->
        val fraction = sector.startTrackPosition.coerceIn(0f, 1f)
        val sampledCenter = centerLine.samplePointAtFraction(fraction)?.toOffset()
        val fallbackHalfWidth = fallbackHalfWidthPx.takeIf { it.isFinite() && it > 1f } ?: 18f
        val calibratedHalfWidth = sector.gateHalfWidthMeters
            ?.takeIf { width -> width.isFinite() && width > 0.1f }
            ?.times(projectionScale)
            ?.takeIf { width -> width.isFinite() && width > 1f }
        val projectedGateCenter = sector.projectGateCenter(
            bounds = bounds,
            canvasSize = canvasSize,
            padding = padding,
        )
        val anchor = resolveSectorBoundaryAnchor(
            fraction = fraction,
            sampledCenter = sampledCenter,
            projectedGateCenter = projectedGateCenter,
            centerLine = centerLine,
            fallbackHalfWidth = fallbackHalfWidth,
            calibratedHalfWidth = calibratedHalfWidth,
        ) ?: return@mapNotNull null
        val localNormal = anchor.direction?.let { Offset(-it.y, it.x).normalizedOrNull() }
        val gateNormal = sector.gateNormalOffset()?.normalizedOrNull()
        val resolvedNormal = localNormal ?: gateNormal ?: return@mapNotNull null
        val sampledEdgeHalfWidths = resolveSectorEdgeHalfWidths(
            fraction = fraction,
            centerOffset = anchor.centerOffset,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            useSegmentDistance = anchor.useSegmentEdgeDistance,
        )
        val leftEdgeHalfWidth = sampledEdgeHalfWidths.first?.takeIf { width -> width.isFinite() && width > 1f }
        val rightEdgeHalfWidth = sampledEdgeHalfWidths.second?.takeIf { width -> width.isFinite() && width > 1f }
        val leftHalfWidth = resolveSectorBoundaryHalfWidth(
            calibratedHalfWidth = calibratedHalfWidth,
            edgeHalfWidth = leftEdgeHalfWidth,
            fallbackHalfWidth = fallbackHalfWidth,
        )
        val rightHalfWidth = resolveSectorBoundaryHalfWidth(
            calibratedHalfWidth = calibratedHalfWidth,
            edgeHalfWidth = rightEdgeHalfWidth,
            fallbackHalfWidth = fallbackHalfWidth,
        )
        val boundaryHalfWidth = maxOf(leftHalfWidth, rightHalfWidth)
        val boundaryOvershootPx = boundaryHalfWidth
            .times(sectorBoundaryOvershootRatio)
            .coerceIn(sectorBoundaryMinimumOvershootPx, sectorBoundaryMaximumOvershootPx)
        SessionAnalysisTrackSectorBoundary(
            label = sector.label,
            start = anchor.centerOffset - resolvedNormal * (boundaryHalfWidth + boundaryOvershootPx),
            end = anchor.centerOffset + resolvedNormal * (boundaryHalfWidth + boundaryOvershootPx),
        )
    }.toImmutableList()
}

/**
 * Places detached sector markers on the outer side of each boundary so labels stay legible without
 * overlapping the track itself.
 */
internal fun buildSectorMarkers(
    boundaries: ImmutableList<SessionAnalysisTrackSectorBoundary>,
    canvasSize: IntSize,
    trackCenter: Offset,
    markerDistancePx: Float,
    markerSpacingPx: Float,
    markerNudgePx: Float,
): ImmutableList<SessionAnalysisTrackSectorMarker> {
    if (boundaries.isEmpty() || canvasSize.width <= 0 || canvasSize.height <= 0) return persistentListOf()
    val placedMarkers = mutableListOf<SessionAnalysisTrackSectorMarker>()
    boundaries.forEachIndexed { index, boundary ->
        val midPoint = Offset(
            x = (boundary.start.x + boundary.end.x) * 0.5f,
            y = (boundary.start.y + boundary.end.y) * 0.5f,
        )
        val boundaryNormal = (boundary.end - boundary.start).normalizedOrNull() ?: Offset.Zero
        val farEdge = if (boundary.end.distanceTo(trackCenter) >= boundary.start.distanceTo(trackCenter)) {
            boundary.end
        } else {
            boundary.start
        }
        val normalDirection = (farEdge - midPoint).normalizedOrNull() ?: boundaryNormal
        val tangentDirection = Offset(-normalDirection.y, normalDirection.x)
        var detachedPoint = (farEdge + normalDirection * markerDistancePx).clampInside(
            canvasSize = canvasSize,
            margin = sectorMarkerCanvasMarginPx,
        )
        if (placedMarkers.any { marker -> marker.point.distanceTo(detachedPoint) < markerSpacingPx }) {
            val tangentSide = if (index % 2 == 0) 1f else -1f
            detachedPoint = (detachedPoint + tangentDirection * (markerNudgePx * tangentSide)).clampInside(
                canvasSize = canvasSize,
                margin = sectorMarkerCanvasMarginPx,
            )
        }
        if (placedMarkers.any { marker -> marker.point.distanceTo(detachedPoint) < markerSpacingPx }) {
            detachedPoint = (detachedPoint + normalDirection * markerDistancePx).clampInside(
                canvasSize = canvasSize,
                margin = sectorMarkerCanvasMarginPx,
            )
        }
        placedMarkers += SessionAnalysisTrackSectorMarker(
            label = if (index == 0) "SF" else boundary.label,
            point = detachedPoint,
        )
    }
    return placedMarkers.filter { marker ->
        marker.point.x in 0f..canvasSize.width.toFloat() &&
            marker.point.y in 0f..canvasSize.height.toFloat()
    }.toImmutableList()
}

internal fun ImmutableList<SessionAnalysisTrackSectorBoundary>.limitSectorBoundaryLength(
    canvasSize: IntSize,
    surfaceStrokePx: Float,
    cameraZoom: Float,
): ImmutableList<SessionAnalysisTrackSectorBoundary> {
    if (isEmpty()) return this
    val maxLengthPx = resolveSectorBoundaryMaxLengthPx(
        canvasSize = canvasSize,
        surfaceStrokePx = surfaceStrokePx,
        cameraZoom = cameraZoom,
    )
    if (!maxLengthPx.isFinite()) return this
    return map { boundary -> boundary.limitLength(maxLengthPx) }.toImmutableList()
}

private fun SessionAnalysisSectorUi.gateNormalOffset(): Offset? {
    val x = gateNormalX?.takeIf(Float::isFinite) ?: return null
    val y = gateNormalY?.takeIf(Float::isFinite) ?: return null
    return Offset(x, y)
}

private fun resolveSectorBoundaryAnchor(
    fraction: Float,
    sampledCenter: Offset?,
    projectedGateCenter: Offset?,
    centerLine: ImmutableList<SessionAnalysisFractionPointUi>,
    fallbackHalfWidth: Float,
    calibratedHalfWidth: Float?,
): SectorBoundaryAnchor? {
    val gateProjection = projectedGateCenter?.let(centerLine::projectNearestTo)
    val useGateProjection = gateProjection?.isUsableForGate(
        fallbackHalfWidth = fallbackHalfWidth,
        calibratedHalfWidth = calibratedHalfWidth,
    ) == true
    val centerOffset = when {
        useGateProjection -> gateProjection.point
        sampledCenter != null -> sampledCenter
        else -> gateProjection?.point
    } ?: return null
    val direction = when {
        useGateProjection -> gateProjection.direction
        else -> centerLine.sampleDirectionAtFraction(fraction) ?: gateProjection?.direction
    }
    return SectorBoundaryAnchor(
        centerOffset = centerOffset,
        direction = direction,
        useSegmentEdgeDistance = useGateProjection || sampledCenter == null,
    )
}

private fun resolveSectorEdgeHalfWidths(
    fraction: Float,
    centerOffset: Offset,
    leftEdge: ImmutableList<SessionAnalysisFractionPointUi>,
    rightEdge: ImmutableList<SessionAnalysisFractionPointUi>,
    useSegmentDistance: Boolean,
): Pair<Float?, Float?> = if (useSegmentDistance) {
    leftEdge.nearestSegmentDistanceTo(centerOffset) to rightEdge.nearestSegmentDistanceTo(centerOffset)
} else {
    leftEdge.samplePointAtFraction(fraction)?.toOffset()?.distanceTo(centerOffset) to
        rightEdge.samplePointAtFraction(fraction)?.toOffset()?.distanceTo(centerOffset)
}

private fun SectorCenterProjection.isUsableForGate(fallbackHalfWidth: Float, calibratedHalfWidth: Float?): Boolean {
    val distance = distanceToSource.takeIf { value -> value.isFinite() } ?: return false
    val maxOffset = maxOf(
        fallbackHalfWidth * 3f,
        (calibratedHalfWidth ?: fallbackHalfWidth) * 2.2f,
        24f,
    )
    return distance <= maxOffset
}

private fun SessionAnalysisSectorUi.projectGateCenter(bounds: Rect, canvasSize: IntSize, padding: Float): Offset? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return null
    val x = gateCenterX?.takeIf(Float::isFinite) ?: return null
    val y = gateCenterY?.takeIf(Float::isFinite) ?: return null
    val scale = resolveProjectionScale(bounds = bounds, canvasSize = canvasSize, padding = padding)
    val drawWidth = bounds.width * scale
    val drawHeight = bounds.height * scale
    val offsetX = (canvasSize.width.toFloat() - drawWidth) * 0.5f
    val offsetY = (canvasSize.height.toFloat() - drawHeight) * 0.5f
    return Offset(
        x = offsetX + (x - bounds.left) * scale,
        y = offsetY + (y - bounds.top) * scale,
    )
}

private fun List<SessionAnalysisFractionPointUi>.projectNearestTo(point: Offset): SectorCenterProjection? {
    if (size < 2) return null
    var bestProjection: SectorCenterProjection? = null
    var bestDistanceSquared = Float.POSITIVE_INFINITY
    for (index in 1..lastIndex) {
        val start = this[index - 1].toOffset()
        val end = this[index].toOffset()
        val localFraction = projectPointOnSegment(pointer = point, start = start, end = end)
        val projected = Offset(
            x = lerp(start.x, end.x, localFraction),
            y = lerp(start.y, end.y, localFraction),
        )
        val distanceSquared = projected.distanceSquaredTo(point)
        if (distanceSquared < bestDistanceSquared) {
            bestDistanceSquared = distanceSquared
            val direction = (end - start).normalizedOrNull()
            if (direction != null) {
                bestProjection = SectorCenterProjection(
                    point = projected,
                    direction = direction,
                    distanceToSource = distanceSquared.fastSqrt(),
                )
            }
        }
    }
    return bestProjection
}

private fun List<SessionAnalysisFractionPointUi>.nearestSegmentDistanceTo(point: Offset): Float? =
    projectNearestTo(point)?.distanceToSource

private fun Float.fastSqrt(): Float {
    if (!isFinite() || this < 0f) return Float.POSITIVE_INFINITY
    return kotlin.math.sqrt(this)
}

private fun resolveSectorBoundaryHalfWidth(
    calibratedHalfWidth: Float?,
    edgeHalfWidth: Float?,
    fallbackHalfWidth: Float,
): Float {
    val candidateHalfWidth = edgeHalfWidth
        ?: calibratedHalfWidth
        ?: fallbackHalfWidth
    val maximumHalfWidth = (fallbackHalfWidth * 1.85f).coerceAtLeast(
        fallbackHalfWidth + sectorBoundaryMaximumOvershootPx,
    )
    return candidateHalfWidth.coerceAtMost(maximumHalfWidth)
}

private fun resolveSectorBoundaryMaxLengthPx(canvasSize: IntSize, surfaceStrokePx: Float, cameraZoom: Float): Float {
    val minSide = minOf(canvasSize.width, canvasSize.height).toFloat()
    if (!minSide.isFinite() || minSide <= 0f) return Float.POSITIVE_INFINITY
    val visibleTrackWidth = (surfaceStrokePx * cameraZoom.coerceAtLeast(1f))
        .takeIf { width -> width.isFinite() && width > 0f }
        ?: return minSide * sectorBoundaryMaxViewportRatio
    return (visibleTrackWidth * sectorBoundaryVisibleTrackWidthRatio).coerceIn(
        minSide * sectorBoundaryMinViewportRatio,
        minSide * sectorBoundaryMaxViewportRatio,
    )
}

private fun SessionAnalysisTrackSectorBoundary.limitLength(maxLengthPx: Float): SessionAnalysisTrackSectorBoundary {
    val segment = end - start
    val length = segment.getDistance()
    if (!length.isFinite() || length <= maxLengthPx) return this
    val midPoint = Offset(
        x = (start.x + end.x) * 0.5f,
        y = (start.y + end.y) * 0.5f,
    )
    val halfSegment = segment.normalizedOrNull()?.times(maxLengthPx * 0.5f) ?: return this
    return copy(
        start = midPoint - halfSegment,
        end = midPoint + halfSegment,
    )
}

private fun Offset.distanceTo(other: Offset): Float = (this - other).getDistance()

private fun Offset.clampInside(canvasSize: IntSize, margin: Float): Offset = Offset(
    x = x.coerceIn(margin, canvasSize.width.toFloat() - margin),
    y = y.coerceIn(margin, canvasSize.height.toFloat() - margin),
)
