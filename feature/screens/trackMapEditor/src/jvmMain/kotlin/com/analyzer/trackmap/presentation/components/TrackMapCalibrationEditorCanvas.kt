package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationCanvasUiState
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun TrackMapCalibrationEditorCanvas(
    uiState: TrackMapCalibrationCanvasUiState,
    modifier: Modifier = Modifier,
    onAddPoint: (Gate) -> Unit,
    onSelectMarker: (String) -> Unit,
    onUpdateGate: (String, Gate) -> Unit,
) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    val extended = SimAnalyzerTheme.extended
    val surfaceColor = material.background
    val palette = TrackMapCalibrationCanvasPalette(
        surfaceColor = surfaceColor,
        baseTrackGlow = material.primary.copy(alpha = 0.18f),
        baseTrackStroke = material.primary.copy(alpha = 0.34f),
        centerLine = material.primary.copy(alpha = 0.9f),
        sectorHighlight = extended.purple,
        liveHalo = material.primary.copy(alpha = 0.16f),
        liveMarker = material.primary,
    )
    val overlayColors = TrackMapCalibrationSelectionOverlayColors(
        labelBackground = material.surface,
        labelText = material.onSurface,
        labelBorder = material.primary.copy(alpha = 0.75f),
        handleCenter = material.background,
    )

    BoxWithConstraints(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(surfaceColor)
            .border(1.dp, chrome.borderSubtle, SimAnalyzerTheme.shapes.large)
            .padding(18.dp),
    ) {
        if (uiState.points.size < 2) {
            Text(
                text = "Track map has no points",
                modifier = Modifier.align(Alignment.Center),
                color = material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            return@BoxWithConstraints
        }

        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        var activeDrag by remember { mutableStateOf<TrackMapCalibrationActiveDrag?>(null) }
        val scene = rememberTrackMapCalibrationEditorCanvasScene(
            uiState = uiState,
            widthPx = widthPx,
            heightPx = heightPx,
            activeDrag = activeDrag,
        ) ?: return@BoxWithConstraints

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    scene.isAddPointMode,
                    scene.selectedMarker?.gateId,
                    scene.markerScreenCenters,
                    scene.sectorScreenPointsByEndGateId,
                ) {
                    detectTapGestures { offset ->
                        scene.handleTap(
                            offset = offset,
                            onAddPoint = onAddPoint,
                            onSelectMarker = onSelectMarker,
                        )
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTrackMapCalibrationCanvasScene(
                    scene = scene,
                    palette = palette,
                )
            }

            TrackMapCalibrationEditorSelectionOverlay(
                uiState = scene.selectionUiState(activeDrag),
                colors = overlayColors,
                onHandleDragStart = { point ->
                    activeDrag = scene.startHandleDrag(point)
                },
                onHandleDrag = { point ->
                    activeDrag = scene.updateHandleDrag(
                        screenPoint = point,
                        activeDrag = activeDrag,
                    )
                },
                onHandleDragEnd = {
                    commitHandleDrag(
                        activeDrag = activeDrag,
                        onUpdateGate = onUpdateGate,
                    )
                    activeDrag = null
                },
            )
        }
    }
}
