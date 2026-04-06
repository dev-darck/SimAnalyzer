package com.analyzer.session.analysis.presentation.components.map.state

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapCameraState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPointerState
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewCanvasState
import com.analyzer.session.analysis.presentation.components.map.support.averageOffset
import com.analyzer.session.analysis.presentation.components.map.support.buildSectorBoundaries
import com.analyzer.session.analysis.presentation.components.map.support.buildSectorMarkers
import com.analyzer.session.analysis.presentation.components.map.support.buildTraceLookup
import com.analyzer.session.analysis.presentation.components.map.support.projectTrackMap
import com.analyzer.session.analysis.presentation.components.map.support.resolveProjectionPadding
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapOverlayMarkerScale
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackSurfaceStrokePx
import com.analyzer.session.analysis.presentation.components.map.support.sectorLabelChipSizeDp
import com.analyzer.session.analysis.presentation.components.map.support.sectorLabelGapDp
import com.analyzer.session.analysis.presentation.components.map.support.sectorLabelNudgeDp
import com.analyzer.session.analysis.presentation.components.map.support.sectorLabelSpacingDp
import com.analyzer.session.analysis.presentation.components.map.support.telemetryPathBreakIndices
import com.analyzer.session.analysis.presentation.components.map.support.trackMapWorldBounds
import com.analyzer.session.analysis.presentation.components.map.support.transformForCamera
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTrackCanvasState
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun rememberSessionAnalysisTrackMapViewportState(
    trackCanvasState: SessionAnalysisTrackCanvasState,
    cursorFraction: Float?,
    cursorFrameId: Long?,
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
    selectionLocked: Boolean,
    focusMode: Boolean,
    canvasSize: IntSize,
): SessionAnalysisTrackMapViewportState {
    val density = LocalDensity.current
    val worldBounds = remember(trackCanvasState) {
        trackCanvasState.trackMapWorldBounds()
    }
    val projectionPadding = remember(trackCanvasState.trackSurfaceWidthMeters, worldBounds, canvasSize) {
        resolveProjectionPadding(
            trackSurfaceWidthMeters = trackCanvasState.trackSurfaceWidthMeters,
            bounds = worldBounds,
            canvasSize = canvasSize,
        )
    }
    val surfaceStrokePx = remember(trackCanvasState.trackSurfaceWidthMeters, worldBounds, canvasSize) {
        resolveTrackSurfaceStrokePx(
            trackSurfaceWidthMeters = trackCanvasState.trackSurfaceWidthMeters,
            bounds = worldBounds,
            canvasSize = canvasSize,
        )
    }

    val projectedTrackMap = remember(trackCanvasState, worldBounds, canvasSize, projectionPadding) {
        trackCanvasState.projectTrackMap(
            bounds = worldBounds,
            canvasSize = canvasSize,
            padding = projectionPadding,
        )
    }
    val projectedCenterLine = projectedTrackMap.centerLine
    val projectedLeftEdge = projectedTrackMap.leftEdge
    val projectedRightEdge = projectedTrackMap.rightEdge
    val projectedSelectedTrace = projectedTrackMap.selectedTrace
    val projectedReferenceTrace = projectedTrackMap.referenceTrace
    val projectedInteractionTrace = projectedTrackMap.interactionTrace
    val hasTrackSurface = projectedLeftEdge.size >= 2 && projectedRightEdge.size >= 2
    val sectorBoundaries = remember(
        trackCanvasState.sectors,
        projectedCenterLine,
        projectedLeftEdge,
        projectedRightEdge,
        surfaceStrokePx,
        worldBounds,
        canvasSize,
        projectionPadding,
    ) {
        buildSectorBoundaries(
            sectors = trackCanvasState.sectors,
            centerLine = projectedCenterLine,
            leftEdge = projectedLeftEdge,
            rightEdge = projectedRightEdge,
            fallbackHalfWidthPx = surfaceStrokePx * 0.48f,
            bounds = worldBounds,
            canvasSize = canvasSize,
            padding = projectionPadding,
        )
    }
    val focusState = rememberSessionAnalysisTrackMapFocusState(
        projectedCenterLine = projectedCenterLine,
        projectedSelectedTrace = projectedSelectedTrace,
        projectedReferenceTrace = projectedReferenceTrace,
        projectedInteractionTrace = projectedInteractionTrace,
        selectedTrailStartFraction = trackCanvasState.selectedTrailStartFraction,
        cursorFraction = cursorFraction,
        cursorFrameId = cursorFrameId,
        activePoint = activePoint,
        activeSample = activeSample,
        selectionLocked = selectionLocked,
        focusMode = focusMode,
        canvasSize = canvasSize,
    )
    val hoverDistancePx = remember(surfaceStrokePx, hasTrackSurface) {
        if (hasTrackSurface) surfaceStrokePx * 0.56f else maxOf(surfaceStrokePx * 0.48f, 14f)
    }
    val selectedTrailTraceBreakIndices = remember(focusState.selectedTrailTrace) {
        focusState.selectedTrailTrace.telemetryPathBreakIndices()
    }
    val projectedSelectedTraceLookup = remember(projectedSelectedTrace) {
        projectedSelectedTrace.buildTraceLookup()
    }
    val hoverTraceLookup = remember(focusState.hoverTrace, projectedSelectedTrace, projectedSelectedTraceLookup) {
        if (focusState.hoverTrace === projectedSelectedTrace) {
            projectedSelectedTraceLookup
        } else {
            focusState.hoverTrace.buildTraceLookup()
        }
    }

    val cameraAnchor = focusState.cameraAnchor
    val cameraFocusPoint = focusState.cameraFocusPoint
    val cameraZoom = focusState.cameraZoom
    val overlayMarkerScale = remember(canvasSize) { resolveTrackMapOverlayMarkerScale(canvasSize) }
    val cameraState = remember(cameraAnchor, cameraFocusPoint, cameraZoom, canvasSize) {
        SessionAnalysisTrackMapCameraState(
            anchor = cameraAnchor,
            focusPoint = cameraFocusPoint,
            zoom = cameraZoom,
            canvasSize = canvasSize,
        )
    }
    val pointerState = remember(
        focusState.selectedTraceAvailable,
        projectedSelectedTrace,
        projectedSelectedTraceLookup,
        focusState.hoverTrace,
        hoverTraceLookup,
        hoverDistancePx,
        focusState.hoverPreferenceIndex,
        focusState.hoverLocalIndexWindow,
        focusState.hoverPreferenceFraction,
        focusMode,
    ) {
        SessionAnalysisTrackMapPointerState(
            selectedTraceAvailable = focusState.selectedTraceAvailable,
            projectedSelectedTrace = projectedSelectedTrace,
            projectedSelectedTraceLookup = projectedSelectedTraceLookup,
            hoverTrace = focusState.hoverTrace,
            hoverTraceLookup = hoverTraceLookup,
            hoverDistancePx = hoverDistancePx,
            hoverPreferenceIndex = focusState.hoverPreferenceIndex,
            hoverLocalIndexWindow = focusState.hoverLocalIndexWindow,
            hoverPreferenceFraction = focusState.hoverPreferenceFraction,
            focusMode = focusMode,
        )
    }
    val trackMapInView = remember(
        projectedTrackMap,
        focusState.selectedTrailTrace,
        sectorBoundaries,
        cameraState,
        surfaceStrokePx,
    ) {
        projectedTrackMap.transformForCamera(
            selectedTrailTrace = focusState.selectedTrailTrace,
            sectorBoundaries = sectorBoundaries,
            cameraState = cameraState,
            surfaceStrokePx = surfaceStrokePx,
        )
    }
    val centerLineInView = trackMapInView.centerLine
    val leftEdgeInView = trackMapInView.leftEdge
    val rightEdgeInView = trackMapInView.rightEdge
    val idealLineInView = trackMapInView.idealLine
    val idealLineBreakIndices = trackMapInView.idealLineBreakIndices
    val selectedTraceInView = trackMapInView.selectedTrace
    val selectedTraceBreakIndices = trackMapInView.selectedTraceBreakIndices
    val referenceTraceInView = trackMapInView.referenceTrace
    val referenceTraceBreakIndices = trackMapInView.referenceTraceBreakIndices
    val sectorBoundariesInView = trackMapInView.sectorBoundaries
    val selectedTrailTraceInView = trackMapInView.selectedTrailTrace
    val activeMarkerInView = remember(
        focusState.activeMarker,
        cameraAnchor,
        cameraFocusPoint,
        canvasSize,
        cameraZoom,
    ) {
        focusState.activeMarker?.transformForCamera(
            anchor = cameraAnchor,
            focusPoint = cameraFocusPoint,
            canvasSize = canvasSize,
            zoom = cameraZoom,
        )
    }
    val sectorMarkersInView =
        remember(sectorBoundariesInView, centerLineInView, canvasSize, density, overlayMarkerScale) {
            val sectorLabelHalfSizePx = with(density) { sectorLabelChipSizeDp.toPx() * 0.5f } * overlayMarkerScale
            val sectorLabelGapPx = with(density) { sectorLabelGapDp.toPx() } * overlayMarkerScale
            val sectorLabelSpacingPx = with(density) { sectorLabelSpacingDp.toPx() } * overlayMarkerScale
            val sectorLabelNudgePx = with(density) { sectorLabelNudgeDp.toPx() } * overlayMarkerScale
            buildSectorMarkers(
                boundaries = sectorBoundariesInView,
                canvasSize = canvasSize,
                trackCenter = centerLineInView.averageOffset(),
                markerDistancePx = sectorLabelHalfSizePx + sectorLabelGapPx,
                markerSpacingPx = sectorLabelSpacingPx,
                markerNudgePx = sectorLabelNudgePx,
            )
        }

    return remember(
        surfaceStrokePx,
        centerLineInView,
        leftEdgeInView,
        rightEdgeInView,
        idealLineInView,
        idealLineBreakIndices,
        selectedTraceInView,
        selectedTraceBreakIndices,
        selectedTrailTraceInView,
        selectedTrailTraceBreakIndices,
        referenceTraceInView,
        referenceTraceBreakIndices,
        sectorBoundariesInView,
        sectorMarkersInView,
        activeMarkerInView,
        cameraState,
        pointerState,
        focusState.displayMarkerDirection,
        focusState.overlayDimAlpha,
        overlayMarkerScale,
    ) {
        SessionAnalysisTrackMapViewportState(
            surfaceStrokePx = surfaceStrokePx,
            centerLineInView = centerLineInView,
            leftEdgeInView = leftEdgeInView,
            rightEdgeInView = rightEdgeInView,
            idealLineInView = idealLineInView,
            idealLineBreakIndices = idealLineBreakIndices,
            selectedTraceInView = selectedTraceInView,
            selectedTraceBreakIndices = selectedTraceBreakIndices,
            selectedTrailTraceInView = selectedTrailTraceInView,
            selectedTrailTraceBreakIndices = selectedTrailTraceBreakIndices,
            referenceTraceInView = referenceTraceInView,
            referenceTraceBreakIndices = referenceTraceBreakIndices,
            sectorBoundariesInView = sectorBoundariesInView,
            sectorMarkersInView = sectorMarkersInView,
            activeMarkerInView = activeMarkerInView,
            displayMarkerDirection = focusState.displayMarkerDirection,
            overlayDimAlpha = focusState.overlayDimAlpha,
            overlayMarkerScale = overlayMarkerScale,
            cameraState = cameraState,
            pointerState = pointerState,
        )
    }
}

@Preview
@Composable
internal fun RememberSessionAnalysisTrackMapViewportStatePreview() {
    val activePoint = sessionAnalysisTrackMapPreviewActivePoint()
    val viewportState = rememberSessionAnalysisTrackMapViewportState(
        trackCanvasState = sessionAnalysisTrackMapPreviewCanvasState(),
        cursorFraction = activePoint.fraction,
        cursorFrameId = activePoint.selectedFrameId,
        activePoint = activePoint,
        activeSample = sessionAnalysisTrackMapPreviewActiveSample(),
        selectionLocked = true,
        focusMode = false,
        canvasSize = IntSize(width = 260, height = 220),
    )

    SimAnalyzerTheme {
        Text(text = viewportState.centerLineInView.size.toString())
    }
}
