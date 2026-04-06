@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.analyzer.session.analysis.presentation.components.map.scene

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.map.draw.drawTrackMapScene
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapCameraState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPalette
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPointerState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapSurfaceDrawState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapTelemetryDrawState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorBoundary
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorMarker
import com.analyzer.session.analysis.presentation.components.map.overlay.SessionAnalysisTrackMapSectorLabels
import com.analyzer.session.analysis.presentation.components.map.overlay.SessionAnalysisTrackMapSpeedOverlay
import com.analyzer.session.analysis.presentation.components.map.path.SessionAnalysisLinearPathFactory
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewCanvasState
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewPalette
import com.analyzer.session.analysis.presentation.components.map.state.rememberSessionAnalysisTrackMapViewportState
import com.analyzer.session.analysis.presentation.components.map.support.SessionAnalysisTrackMapPointerPurpose
import com.analyzer.session.analysis.presentation.components.map.support.buildTrackSurfacePath
import com.analyzer.session.analysis.presentation.components.map.support.isClosedTelemetryLoop
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapStrokeWidths
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapViewportPointerTarget
import com.analyzer.session.analysis.presentation.components.map.support.toOffsets
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SessionAnalysisTrackMapScene(
    palette: SessionAnalysisTrackMapPalette,
    centerLineInView: ImmutableList<SessionAnalysisFractionPointUi>,
    leftEdgeInView: ImmutableList<SessionAnalysisFractionPointUi>,
    rightEdgeInView: ImmutableList<SessionAnalysisFractionPointUi>,
    surfaceStrokePx: Float,
    sectorBoundariesInView: ImmutableList<SessionAnalysisTrackSectorBoundary>,
    referenceTraceInView: ImmutableList<SessionAnalysisFractionPointUi>,
    referenceTraceBreakIndices: Set<Int>,
    idealLineInView: ImmutableList<SessionAnalysisFractionPointUi>,
    idealLineBreakIndices: Set<Int>,
    selectedTraceInView: ImmutableList<SessionAnalysisFractionPointUi>,
    selectedTraceBreakIndices: Set<Int>,
    selectedTrailTraceInView: ImmutableList<SessionAnalysisFractionPointUi>,
    selectedTrailTraceBreakIndices: Set<Int>,
    activeMarkerInView: SessionAnalysisFractionPointUi?,
    activeSample: SessionAnalysisSampleUi?,
    displayMarkerDirection: Offset?,
    sectorMarkersInView: ImmutableList<SessionAnalysisTrackSectorMarker>,
    overlayMarkerScale: Float,
    cameraState: SessionAnalysisTrackMapCameraState,
    pointerState: SessionAnalysisTrackMapPointerState,
    selectionLocked: Boolean,
    modifier: Modifier = Modifier,
    onSizeChanged: (IntSize) -> Unit,
    onHover: (Float?, Long?) -> Unit,
    onPress: (Float?, Long?) -> Unit,
) {
    var lastHoverPointer by remember { mutableStateOf<Offset?>(null) }
    var lastHoverTarget by remember { mutableStateOf<TrackMapHoverTarget?>(null) }
    val strokeScale = remember(cameraState.zoom) { cameraState.zoom.coerceAtLeast(1f) }
    val strokeWidths = remember(surfaceStrokePx, strokeScale) {
        resolveTrackMapStrokeWidths(
            surfaceStrokePx = surfaceStrokePx,
            zoom = strokeScale,
        )
    }
    val pathFactory = remember { SessionAnalysisLinearPathFactory() }
    val centerOffsets = remember(centerLineInView) { centerLineInView.toOffsets() }
    val leftEdgeOffsets = remember(leftEdgeInView) { leftEdgeInView.toOffsets() }
    val rightEdgeOffsets = remember(rightEdgeInView) { rightEdgeInView.toOffsets() }
    val trackSurfacePath = remember(leftEdgeInView, rightEdgeInView) {
        buildTrackSurfacePath(leftEdgeInView, rightEdgeInView)
    }
    val trackSurfaceBasePath = remember(centerOffsets, centerLineInView) {
        pathFactory.create(
            points = centerOffsets,
            closed = centerLineInView.isClosedTelemetryLoop(),
        )
    }
    val leftEdgePath = remember(leftEdgeOffsets, leftEdgeInView) {
        pathFactory.create(
            points = leftEdgeOffsets,
            closed = leftEdgeInView.isClosedTelemetryLoop(),
        )
    }
    val rightEdgePath = remember(rightEdgeOffsets, rightEdgeInView) {
        pathFactory.create(
            points = rightEdgeOffsets,
            closed = rightEdgeInView.isClosedTelemetryLoop(),
        )
    }
    val idealLinePath = remember(idealLineInView, idealLineBreakIndices) {
        pathFactory.create(
            points = idealLineInView.toOffsets(),
            closed = idealLineInView.isClosedTelemetryLoop(),
            breakIndices = idealLineBreakIndices,
        )
    }
    val referenceLinePath = remember(referenceTraceInView, referenceTraceBreakIndices) {
        pathFactory.create(
            points = referenceTraceInView.toOffsets(),
            closed = referenceTraceInView.isClosedTelemetryLoop(),
            breakIndices = referenceTraceBreakIndices,
        )
    }
    val selectedBasePath = remember(selectedTraceInView, selectedTraceBreakIndices) {
        pathFactory.create(
            points = selectedTraceInView.toOffsets(),
            closed = selectedTraceInView.isClosedTelemetryLoop(),
            breakIndices = selectedTraceBreakIndices,
        )
    }
    val selectedTrailPath = remember(selectedTrailTraceInView, selectedTrailTraceBreakIndices) {
        pathFactory.create(
            points = selectedTrailTraceInView.toOffsets(),
            breakIndices = selectedTrailTraceBreakIndices,
        )
    }
    val surfaceDrawState = remember(
        trackSurfaceBasePath,
        trackSurfacePath,
        palette.trackSurfaceColor,
        leftEdgePath,
        rightEdgePath,
        palette.trackEdgeColor,
        centerLineInView,
        surfaceStrokePx,
        strokeScale,
    ) {
        SessionAnalysisTrackMapSurfaceDrawState(
            trackSurfaceBasePath = trackSurfaceBasePath,
            trackSurfacePath = trackSurfacePath,
            clipTelemetryToSurface = false,
            showSurfaceEdges = leftEdgeInView.size >= 2 && rightEdgeInView.size >= 2,
            trackSurfaceColor = palette.trackSurfaceColor,
            leftEdgePath = leftEdgePath,
            rightEdgePath = rightEdgePath,
            trackEdgeColor = palette.trackEdgeColor,
            centerLineInView = centerLineInView,
            surfaceStrokePx = surfaceStrokePx,
            strokeScale = strokeScale,
        )
    }
    val telemetryDrawState = remember(
        sectorBoundariesInView,
        referenceTraceInView,
        referenceLinePath,
        idealLineInView,
        idealLinePath,
        selectedTraceInView,
        selectedBasePath,
        selectedTrailTraceInView,
        selectedTrailPath,
        activeMarkerInView,
        displayMarkerDirection,
        strokeWidths,
    ) {
        SessionAnalysisTrackMapTelemetryDrawState(
            sectorBoundariesInView = sectorBoundariesInView,
            referenceTraceInView = referenceTraceInView,
            referenceLinePath = referenceLinePath,
            idealLineInView = idealLineInView,
            idealLinePath = idealLinePath,
            selectedTraceInView = selectedTraceInView,
            selectedBasePath = selectedBasePath,
            selectedTrailTraceInView = selectedTrailTraceInView,
            selectedTrailPath = selectedTrailPath,
            activeMarkerInView = activeMarkerInView,
            displayMarkerDirection = displayMarkerDirection,
            strokeWidths = strokeWidths,
        )
    }

    Box(
        modifier = modifier
            .onSizeChanged(onSizeChanged)
            .onPointerEvent(PointerEventType.Move) { event ->
                if (selectionLocked) return@onPointerEvent
                val pointer = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                if (shouldSkipTrackMapHoverUpdate(
                        previous = lastHoverPointer,
                        current = pointer,
                        focusMode = pointerState.focusMode,
                    )
                ) {
                    return@onPointerEvent
                }
                lastHoverPointer = pointer
                val hoverTarget = resolveTrackMapViewportPointerTarget(
                    pointer = pointer,
                    cameraState = cameraState,
                    pointerState = pointerState,
                    purpose = SessionAnalysisTrackMapPointerPurpose.Hover,
                )
                val resolvedHoverTarget = TrackMapHoverTarget(
                    fraction = hoverTarget.first,
                    frameId = hoverTarget.second,
                )
                if (lastHoverTarget.isEquivalentTo(resolvedHoverTarget)) {
                    return@onPointerEvent
                }
                lastHoverTarget = resolvedHoverTarget
                onHover(resolvedHoverTarget.fraction, resolvedHoverTarget.frameId)
            }
            .onPointerEvent(PointerEventType.Exit) {
                lastHoverPointer = null
                if (!selectionLocked && lastHoverTarget != null) {
                    lastHoverTarget = null
                    onHover(null, null)
                }
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                lastHoverPointer = null
                lastHoverTarget = null
                val pointer = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                val pressTarget = resolveTrackMapViewportPointerTarget(
                    pointer = pointer,
                    cameraState = cameraState,
                    pointerState = pointerState,
                    purpose = SessionAnalysisTrackMapPointerPurpose.Press,
                )
                onPress(pressTarget.first, pressTarget.second)
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            drawTrackMapScene(
                surfaceState = surfaceDrawState,
                telemetryState = telemetryDrawState,
                palette = palette,
            )
        }

        SessionAnalysisTrackMapSpeedOverlay(
            activeMarkerInView = activeMarkerInView,
            activeSample = activeSample,
            overlayBg = palette.overlayBg,
            overlayFg = palette.overlayFg,
        )
        SessionAnalysisTrackMapSectorLabels(
            markers = sectorMarkersInView,
            palette = palette,
            markerScale = overlayMarkerScale,
        )
    }
}

@Preview
@Composable
internal fun SessionAnalysisTrackMapScenePreview() {
    val trackCanvasState = sessionAnalysisTrackMapPreviewCanvasState()
    val activePoint = sessionAnalysisTrackMapPreviewActivePoint()
    val activeSample = sessionAnalysisTrackMapPreviewActiveSample()
    val viewportState = rememberSessionAnalysisTrackMapViewportState(
        trackCanvasState = trackCanvasState,
        cursorFraction = activePoint.fraction,
        cursorFrameId = activePoint.selectedFrameId,
        activePoint = activePoint,
        activeSample = activeSample,
        selectionLocked = true,
        focusMode = false,
        canvasSize = IntSize(width = 260, height = 220),
    )

    SimAnalyzerTheme {
        SessionAnalysisTrackMapScene(
            modifier = Modifier.size(width = 260.dp, height = 220.dp),
            palette = sessionAnalysisTrackMapPreviewPalette(),
            centerLineInView = viewportState.centerLineInView,
            leftEdgeInView = viewportState.leftEdgeInView,
            rightEdgeInView = viewportState.rightEdgeInView,
            surfaceStrokePx = viewportState.surfaceStrokePx,
            sectorBoundariesInView = viewportState.sectorBoundariesInView,
            referenceTraceInView = viewportState.referenceTraceInView,
            referenceTraceBreakIndices = viewportState.referenceTraceBreakIndices,
            idealLineInView = viewportState.idealLineInView,
            idealLineBreakIndices = viewportState.idealLineBreakIndices,
            selectedTraceInView = viewportState.selectedTraceInView,
            selectedTraceBreakIndices = viewportState.selectedTraceBreakIndices,
            selectedTrailTraceInView = viewportState.selectedTrailTraceInView,
            selectedTrailTraceBreakIndices = viewportState.selectedTrailTraceBreakIndices,
            activeMarkerInView = viewportState.activeMarkerInView,
            activeSample = activeSample,
            displayMarkerDirection = viewportState.displayMarkerDirection,
            sectorMarkersInView = viewportState.sectorMarkersInView,
            overlayMarkerScale = viewportState.overlayMarkerScale,
            cameraState = viewportState.cameraState,
            pointerState = viewportState.pointerState,
            selectionLocked = true,
            onSizeChanged = {},
            onHover = { _, _ -> },
            onPress = { _, _ -> },
        )
    }
}

private fun shouldSkipTrackMapHoverUpdate(previous: Offset?, current: Offset, focusMode: Boolean): Boolean {
    val lastPointer = previous ?: return false
    val minimumDistancePx = if (focusMode) 5f else 2f
    return (current - lastPointer).getDistance() < minimumDistancePx
}

private data class TrackMapHoverTarget(val fraction: Float?, val frameId: Long?)

private fun TrackMapHoverTarget?.isEquivalentTo(other: TrackMapHoverTarget): Boolean {
    val current = this ?: return false
    if (current.frameId != other.frameId) return false
    val currentFraction = current.fraction
    val nextFraction = other.fraction
    if (currentFraction == null || nextFraction == null) {
        return currentFraction == nextFraction
    }
    return kotlin.math.abs(currentFraction - nextFraction) < 0.0005f
}
