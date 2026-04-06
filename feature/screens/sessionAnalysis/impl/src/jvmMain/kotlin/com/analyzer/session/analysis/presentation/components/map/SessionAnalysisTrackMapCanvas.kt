package com.analyzer.session.analysis.presentation.components.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.map.overlay.SessionAnalysisTrackMapDiagnosticOverlay
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewCanvasState
import com.analyzer.session.analysis.presentation.components.map.scene.SessionAnalysisTrackMapScene
import com.analyzer.session.analysis.presentation.components.map.state.rememberSessionAnalysisTrackMapViewportState
import com.analyzer.session.analysis.presentation.components.map.support.rememberSessionAnalysisTrackMapPalette
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTrackCanvasState
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_workspace_map_empty
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Owns track-map rendering and pointer interaction so the rest of the UI consumes one focused map surface.
 */
@Composable
internal fun SessionAnalysisTrackMapCanvas(
    trackCanvasState: SessionAnalysisTrackCanvasState?,
    cursorFraction: Float?,
    cursorFrameId: Long?,
    selectionLocked: Boolean,
    focusMode: Boolean,
    modifier: Modifier = Modifier,
    activePoint: SessionAnalysisComparisonPointUi? = null,
    activeSample: SessionAnalysisSampleUi? = null,
    onHoverFraction: (Float?, Long?) -> Unit = { _, _ -> },
    onPressFraction: (Float?, Long?) -> Unit = { _, _ -> },
) {
    if (trackCanvasState == null) {
        SessionAnalysisTrackMapEmptyState(modifier = modifier)
        return
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val onHoverState by rememberUpdatedState(onHoverFraction)
    val onPressState by rememberUpdatedState(onPressFraction)
    val viewportState = rememberSessionAnalysisTrackMapViewportState(
        trackCanvasState = trackCanvasState,
        cursorFraction = cursorFraction,
        cursorFrameId = cursorFrameId,
        activePoint = activePoint,
        activeSample = activeSample,
        selectionLocked = selectionLocked,
        focusMode = focusMode,
        canvasSize = canvasSize,
    )
    val palette = rememberSessionAnalysisTrackMapPalette(
        overlayDimAlpha = viewportState.overlayDimAlpha,
    )

    Box(modifier = modifier) {
        SessionAnalysisTrackMapScene(
            modifier = Modifier.fillMaxSize(),
            palette = palette,
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
            selectionLocked = selectionLocked,
            onSizeChanged = { canvasSize = it },
            onHover = onHoverState,
            onPress = onPressState,
        )
        SessionAnalysisTrackMapDiagnosticOverlay(
            trackCanvasState = trackCanvasState,
            viewportState = viewportState,
            activeTrackPosition = activePoint?.trackPosition ?: activeSample?.trackPosition ?: cursorFraction,
            selectionLocked = selectionLocked,
        )
    }
}

@Preview
@Composable
internal fun SessionAnalysisTrackMapCanvasPreview() {
    SimAnalyzerTheme {
        SessionAnalysisTrackMapCanvas(
            trackCanvasState = sessionAnalysisTrackMapPreviewCanvasState(),
            cursorFraction = sessionAnalysisTrackMapPreviewActivePoint().fraction,
            cursorFrameId = sessionAnalysisTrackMapPreviewActivePoint().selectedFrameId,
            selectionLocked = true,
            focusMode = false,
            activePoint = sessionAnalysisTrackMapPreviewActivePoint(),
            activeSample = sessionAnalysisTrackMapPreviewActiveSample(),
            modifier = Modifier.size(width = 280.dp, height = 220.dp),
        )
    }
}

@Composable
private fun SessionAnalysisTrackMapEmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = SimAnalyzerTheme.chrome.fillMuted,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.session_analysis_workspace_map_empty),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodyMedium,
            )
        }
    }
}
