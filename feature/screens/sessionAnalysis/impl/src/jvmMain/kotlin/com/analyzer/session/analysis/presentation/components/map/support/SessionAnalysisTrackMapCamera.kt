package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisProjectedTrackMap
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapCameraState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapInView
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorBoundary
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun ImmutableList<SessionAnalysisFractionPointUi>.transformPointsForCamera(
    anchor: Offset?,
    focusPoint: Offset?,
    canvasSize: IntSize,
    zoom: Float,
): ImmutableList<SessionAnalysisFractionPointUi> {
    if (isEmpty()) return this
    return map { point ->
        point.transformForCamera(anchor = anchor, focusPoint = focusPoint, canvasSize = canvasSize, zoom = zoom)
    }.toImmutableList()
}

internal fun ImmutableList<SessionAnalysisTrackSectorBoundary>.transformBoundariesForCamera(
    anchor: Offset?,
    focusPoint: Offset?,
    canvasSize: IntSize,
    zoom: Float,
): ImmutableList<SessionAnalysisTrackSectorBoundary> {
    if (isEmpty()) return this
    return map { boundary ->
        boundary.copy(
            start = boundary.start.transformForCamera(
                anchor = anchor,
                focusPoint = focusPoint,
                canvasSize = canvasSize,
                zoom = zoom,
            ),
            end = boundary.end.transformForCamera(
                anchor = anchor,
                focusPoint = focusPoint,
                canvasSize = canvasSize,
                zoom = zoom,
            ),
        )
    }.toImmutableList()
}

internal fun SessionAnalysisProjectedTrackMap.transformForCamera(
    selectedTrailTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    sectorBoundaries: ImmutableList<SessionAnalysisTrackSectorBoundary>,
    cameraState: SessionAnalysisTrackMapCameraState,
    surfaceStrokePx: Float,
): SessionAnalysisTrackMapInView = SessionAnalysisTrackMapInView(
    centerLine = centerLine.transformPointsForCamera(
        anchor = cameraState.anchor,
        focusPoint = cameraState.focusPoint,
        canvasSize = cameraState.canvasSize,
        zoom = cameraState.zoom,
    ),
    leftEdge = leftEdge.transformPointsForCamera(
        anchor = cameraState.anchor,
        focusPoint = cameraState.focusPoint,
        canvasSize = cameraState.canvasSize,
        zoom = cameraState.zoom,
    ),
    rightEdge = rightEdge.transformPointsForCamera(
        anchor = cameraState.anchor,
        focusPoint = cameraState.focusPoint,
        canvasSize = cameraState.canvasSize,
        zoom = cameraState.zoom,
    ),
    idealLine = idealLine.transformPointsForCamera(
        anchor = cameraState.anchor,
        focusPoint = cameraState.focusPoint,
        canvasSize = cameraState.canvasSize,
        zoom = cameraState.zoom,
    ),
    idealLineBreakIndices = idealLineBreakIndices,
    selectedTrace = selectedTrace.transformPointsForCamera(
        anchor = cameraState.anchor,
        focusPoint = cameraState.focusPoint,
        canvasSize = cameraState.canvasSize,
        zoom = cameraState.zoom,
    ),
    selectedTraceBreakIndices = selectedTraceBreakIndices,
    selectedTrailTrace = selectedTrailTrace.transformPointsForCamera(
        anchor = cameraState.anchor,
        focusPoint = cameraState.focusPoint,
        canvasSize = cameraState.canvasSize,
        zoom = cameraState.zoom,
    ),
    referenceTrace = referenceTrace.transformPointsForCamera(
        anchor = cameraState.anchor,
        focusPoint = cameraState.focusPoint,
        canvasSize = cameraState.canvasSize,
        zoom = cameraState.zoom,
    ),
    referenceTraceBreakIndices = referenceTraceBreakIndices,
    sectorBoundaries = sectorBoundaries.transformBoundariesForCamera(
        anchor = cameraState.anchor,
        focusPoint = cameraState.focusPoint,
        canvasSize = cameraState.canvasSize,
        zoom = cameraState.zoom,
    ).limitSectorBoundaryLength(
        canvasSize = cameraState.canvasSize,
        surfaceStrokePx = surfaceStrokePx,
        cameraZoom = cameraState.zoom,
    ),
)

internal fun SessionAnalysisFractionPointUi.transformForCamera(
    anchor: Offset?,
    focusPoint: Offset?,
    canvasSize: IntSize,
    zoom: Float,
): SessionAnalysisFractionPointUi {
    val transformed = toOffset().transformForCamera(
        anchor = anchor,
        focusPoint = focusPoint,
        canvasSize = canvasSize,
        zoom = zoom,
    )
    return copy(x = transformed.x, y = transformed.y)
}

internal fun Offset.transformForCamera(
    anchor: Offset?,
    focusPoint: Offset?,
    canvasSize: IntSize,
    zoom: Float,
): Offset {
    val safeAnchor = anchor ?: return this
    if (!canApplyCameraTransform(canvasSize = canvasSize, zoom = zoom)) return this
    val viewportCenter = focusPoint ?: Offset(x = canvasSize.width * 0.5f, y = canvasSize.height * 0.5f)
    return viewportCenter + (this - safeAnchor) * zoom
}

internal fun Offset.toCameraBaseSpace(anchor: Offset?, focusPoint: Offset?, canvasSize: IntSize, zoom: Float): Offset {
    val safeAnchor = anchor ?: return this
    if (!canApplyCameraTransform(canvasSize = canvasSize, zoom = zoom)) return this
    val viewportCenter = focusPoint ?: Offset(x = canvasSize.width * 0.5f, y = canvasSize.height * 0.5f)
    return safeAnchor + (this - viewportCenter) / zoom
}

private fun canApplyCameraTransform(canvasSize: IntSize, zoom: Float): Boolean {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return false
    return zoom > 1.001f
}
