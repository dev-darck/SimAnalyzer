package com.analyzer.session.analysis.presentation.components.map.state

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewCanvasState
import com.analyzer.session.analysis.presentation.components.map.support.buildSelectedTrailTrace
import com.analyzer.session.analysis.presentation.components.map.support.indexOfFrameId
import com.analyzer.session.analysis.presentation.components.map.support.nearestIndexToFraction
import com.analyzer.session.analysis.presentation.components.map.support.nearestToFraction
import com.analyzer.session.analysis.presentation.components.map.support.nearestToFrameId
import com.analyzer.session.analysis.presentation.components.map.support.projectTrackMap
import com.analyzer.session.analysis.presentation.components.map.support.resolveProjectionPadding
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrailMarkerFraction
import com.analyzer.session.analysis.presentation.components.map.support.sampleDirectionAtFraction
import com.analyzer.session.analysis.presentation.components.map.support.sampleDirectionAtIndex
import com.analyzer.session.analysis.presentation.components.map.support.toOffset
import com.analyzer.session.analysis.presentation.components.map.support.trackMapWorldBounds
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val focusCameraFollowDurationMs: Int = 90
private const val focusCameraZoomDurationMs: Int = 120

@Composable
internal fun rememberSessionAnalysisTrackMapFocusState(
    projectedCenterLine: ImmutableList<SessionAnalysisFractionPointUi>,
    projectedSelectedTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    projectedReferenceTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    projectedInteractionTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    selectedTrailStartFraction: Float?,
    cursorFraction: Float?,
    cursorFrameId: Long?,
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
    selectionLocked: Boolean,
    focusMode: Boolean,
    canvasSize: IntSize,
): SessionAnalysisTrackMapFocusState {
    val selectedTraceAvailable = projectedSelectedTrace.size >= 2
    val hoverTrace = remember(
        projectedCenterLine,
        projectedSelectedTrace,
        projectedReferenceTrace,
        projectedInteractionTrace,
    ) {
        when {
            selectedTraceAvailable -> projectedSelectedTrace
            projectedReferenceTrace.size >= 2 -> projectedReferenceTrace
            projectedInteractionTrace.size >= 2 -> projectedInteractionTrace
            projectedCenterLine.size >= 2 -> projectedCenterLine
            else -> persistentListOf()
        }
    }
    val markerFraction = remember(
        cursorFraction,
        activePoint?.fraction,
        activeSample?.trackPosition,
    ) {
        cursorFraction
            ?: activePoint?.fraction
            ?: activeSample?.trackPosition
    }
    val markerFrameId = remember(cursorFrameId, activePoint?.selectedFrameId, activeSample?.frameId) {
        cursorFrameId ?: activePoint?.selectedFrameId ?: activeSample?.frameId
    }
    val hoverPreferenceFraction = cursorFraction ?: activePoint?.fraction ?: activeSample?.trackPosition
    val hoverPreferenceIndex = remember(markerFrameId, markerFraction, projectedSelectedTrace) {
        projectedSelectedTrace.indexOfFrameId(markerFrameId)
            ?: projectedSelectedTrace.nearestIndexToFraction(markerFraction)
    }
    val hoverLocalIndexWindow = if (focusMode) 28 else 96
    val selectedMarker = remember(markerFrameId, markerFraction, projectedSelectedTrace) {
        projectedSelectedTrace.nearestToFrameId(markerFrameId)
            ?: projectedSelectedTrace.nearestToFraction(markerFraction)
    }
    val activeMarker = remember(
        selectedTraceAvailable,
        markerFraction,
        selectedMarker,
        projectedReferenceTrace,
        projectedCenterLine,
        projectedInteractionTrace,
    ) {
        if (selectedTraceAvailable) {
            selectedMarker
        } else {
            selectedMarker
                ?: projectedReferenceTrace.nearestToFraction(markerFraction)
                ?: projectedCenterLine.nearestToFraction(markerFraction)
                ?: projectedInteractionTrace.nearestToFraction(markerFraction)
        }
    }
    val displayMarkerDirection = remember(
        selectedTraceAvailable,
        markerFrameId,
        activeMarker?.fraction,
        hoverPreferenceIndex,
        projectedSelectedTrace,
        projectedReferenceTrace,
        projectedCenterLine,
    ) {
        if (selectedTraceAvailable) {
            projectedSelectedTrace.sampleDirectionAtIndex(
                projectedSelectedTrace.indexOfFrameId(markerFrameId)
                    ?: hoverPreferenceIndex,
            )
        } else {
            val fraction = activeMarker?.fraction ?: return@remember null
            projectedSelectedTrace.sampleDirectionAtFraction(fraction)
                ?: projectedReferenceTrace.sampleDirectionAtFraction(fraction)
                ?: projectedCenterLine.sampleDirectionAtFraction(fraction)
        }
    }
    val targetCameraZoom = if (focusMode && activeMarker != null) 3.2f else 1f
    val cameraZoom by animateFloatAsState(
        targetValue = targetCameraZoom,
        animationSpec = if (focusMode && activeMarker != null) {
            tween(durationMillis = focusCameraZoomDurationMs)
        } else {
            snap()
        },
        label = "trackCameraZoom",
    )
    val targetCameraAnchor = if (focusMode) activeMarker?.toOffset() else null
    val targetCameraFocusPoint = remember(focusMode, selectionLocked, canvasSize, displayMarkerDirection) {
        if (!focusMode || canvasSize.width <= 0 || canvasSize.height <= 0) {
            null
        } else if (!selectionLocked) {
            Offset(x = canvasSize.width * 0.5f, y = canvasSize.height * 0.5f)
        } else {
            val viewportCenter = Offset(x = canvasSize.width * 0.5f, y = canvasSize.height * 0.5f)
            val direction = displayMarkerDirection ?: Offset.Zero
            Offset(
                x = (viewportCenter.x - direction.x * canvasSize.width * 0.18f)
                    .coerceIn(canvasSize.width * 0.24f, canvasSize.width * 0.76f),
                y = (viewportCenter.y - direction.y * canvasSize.height * 0.10f)
                    .coerceIn(canvasSize.height * 0.28f, canvasSize.height * 0.72f),
            )
        }
    }
    val shouldAnimateCameraFollow = focusMode && selectionLocked && targetCameraAnchor != null
    val cameraAnchorX by animateFloatAsState(
        targetValue = targetCameraAnchor?.x ?: 0f,
        animationSpec = if (shouldAnimateCameraFollow) {
            tween(durationMillis = focusCameraFollowDurationMs)
        } else {
            snap()
        },
        label = "trackCameraAnchorX",
    )
    val cameraAnchorY by animateFloatAsState(
        targetValue = targetCameraAnchor?.y ?: 0f,
        animationSpec = if (shouldAnimateCameraFollow) {
            tween(durationMillis = focusCameraFollowDurationMs)
        } else {
            snap()
        },
        label = "trackCameraAnchorY",
    )
    val cameraFocusX by animateFloatAsState(
        targetValue = targetCameraFocusPoint?.x ?: (canvasSize.width * 0.5f),
        animationSpec = if (shouldAnimateCameraFollow && targetCameraFocusPoint != null) {
            tween(durationMillis = focusCameraFollowDurationMs)
        } else {
            snap()
        },
        label = "trackCameraFocusX",
    )
    val cameraFocusY by animateFloatAsState(
        targetValue = targetCameraFocusPoint?.y ?: (canvasSize.height * 0.5f),
        animationSpec = if (shouldAnimateCameraFollow && targetCameraFocusPoint != null) {
            tween(durationMillis = focusCameraFollowDurationMs)
        } else {
            snap()
        },
        label = "trackCameraFocusY",
    )
    val cameraAnchor = if (focusMode && targetCameraAnchor != null) {
        Offset(x = cameraAnchorX, y = cameraAnchorY)
    } else {
        null
    }
    val cameraFocusPoint = if (focusMode && canvasSize.width > 0 && canvasSize.height > 0) {
        Offset(x = cameraFocusX, y = cameraFocusY)
    } else {
        null
    }
    val overlayDimAlpha = if (focusMode) 0.52f else 1f
    val selectedTrailFraction = remember(markerFrameId, markerFraction, projectedSelectedTrace) {
        projectedSelectedTrace.resolveTrailMarkerFraction(
            markerFrameId = markerFrameId,
            markerFraction = markerFraction,
        )?.coerceIn(0f, 1f)
    }
    val selectedTrailTrace = remember(projectedSelectedTrace, selectedTrailFraction, selectedTrailStartFraction) {
        projectedSelectedTrace.buildSelectedTrailTrace(
            trailFraction = selectedTrailFraction,
            trailStartFraction = selectedTrailStartFraction,
        )
    }

    return remember(
        selectedTraceAvailable,
        hoverTrace,
        activeMarker,
        displayMarkerDirection,
        cameraAnchor,
        cameraFocusPoint,
        cameraZoom,
        overlayDimAlpha,
        hoverPreferenceIndex,
        hoverLocalIndexWindow,
        hoverPreferenceFraction,
        selectedTrailTrace,
    ) {
        SessionAnalysisTrackMapFocusState(
            selectedTraceAvailable = selectedTraceAvailable,
            hoverTrace = hoverTrace,
            activeMarker = activeMarker,
            displayMarkerDirection = displayMarkerDirection,
            cameraAnchor = cameraAnchor,
            cameraFocusPoint = cameraFocusPoint,
            cameraZoom = cameraZoom,
            overlayDimAlpha = overlayDimAlpha,
            hoverPreferenceIndex = hoverPreferenceIndex,
            hoverLocalIndexWindow = hoverLocalIndexWindow,
            hoverPreferenceFraction = hoverPreferenceFraction,
            selectedTrailTrace = selectedTrailTrace,
        )
    }
}

@Preview
@Composable
internal fun RememberSessionAnalysisTrackMapFocusStatePreview() {
    val trackCanvasState = sessionAnalysisTrackMapPreviewCanvasState()
    val activePoint = sessionAnalysisTrackMapPreviewActivePoint()
    val activeSample = sessionAnalysisTrackMapPreviewActiveSample()
    val canvasSize = IntSize(width = 260, height = 220)
    val worldBounds = remember(trackCanvasState) {
        trackCanvasState.trackMapWorldBounds()
    }
    val padding = remember(trackCanvasState.trackSurfaceWidthMeters, worldBounds, canvasSize) {
        resolveProjectionPadding(
            trackSurfaceWidthMeters = trackCanvasState.trackSurfaceWidthMeters,
            bounds = worldBounds,
            canvasSize = canvasSize,
        )
    }
    val projectedTrackMap = trackCanvasState.projectTrackMap(
        bounds = worldBounds,
        canvasSize = canvasSize,
        padding = padding,
    )
    val focusState = rememberSessionAnalysisTrackMapFocusState(
        projectedCenterLine = projectedTrackMap.centerLine,
        projectedSelectedTrace = projectedTrackMap.selectedTrace,
        projectedReferenceTrace = projectedTrackMap.referenceTrace,
        projectedInteractionTrace = projectedTrackMap.interactionTrace,
        selectedTrailStartFraction = trackCanvasState.selectedTrailStartFraction,
        cursorFraction = activePoint.fraction,
        cursorFrameId = activePoint.selectedFrameId,
        activePoint = activePoint,
        activeSample = activeSample,
        selectionLocked = true,
        focusMode = true,
        canvasSize = canvasSize,
    )

    SimAnalyzerTheme {
        Text(text = focusState.activeMarker?.fraction?.toString() ?: "no-marker")
    }
}
