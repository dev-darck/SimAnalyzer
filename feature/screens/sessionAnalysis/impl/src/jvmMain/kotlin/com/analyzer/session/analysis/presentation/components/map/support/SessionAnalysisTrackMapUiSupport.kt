package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapCameraState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPalette
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPointerState
import com.project.analyzer.theme.SimAnalyzerTheme

private const val TRACK_MAP_PANEL_ALPHA = 0.38f
private const val TRACK_MAP_CANVAS_ALPHA = 0.18f
private const val TRACK_MAP_SURFACE_ALPHA = 0.62f
private const val TRACK_MAP_EDGE_ALPHA = 0.22f
private const val TRACK_MAP_LINE_CASING_ALPHA = 0.88f
private const val TRACK_MAP_SELECTED_ALPHA = 0.98f
private const val TRACK_MAP_REFERENCE_ALPHA = 0.66f
private const val TRACK_MAP_IDEAL_ALPHA = 0.96f
private const val TRACK_MAP_OVERLAY_ALPHA = 0.92f
private const val TRACK_MAP_MARKER_RING_ALPHA = 0.92f
private const val TRACK_MAP_SECTOR_LABEL_ALPHA = 0.9f

@Composable
internal fun rememberSessionAnalysisTrackMapPalette(overlayDimAlpha: Float): SessionAnalysisTrackMapPalette {
    val background = SimAnalyzerTheme.material.background
    val surface = SimAnalyzerTheme.material.surface
    val surfaceVariant = SimAnalyzerTheme.material.surfaceVariant
    val onSurface = SimAnalyzerTheme.material.onSurface
    val onSurfaceVariant = SimAnalyzerTheme.material.onSurfaceVariant
    val amber = SimAnalyzerTheme.extended.amber
    val cyan = SimAnalyzerTheme.extended.cyan
    val teal = SimAnalyzerTheme.extended.teal

    return remember(
        background,
        surface,
        surfaceVariant,
        onSurface,
        onSurfaceVariant,
        amber,
        cyan,
        teal,
        overlayDimAlpha,
    ) {
        SessionAnalysisTrackMapPalette(
            panelBg = background.copy(alpha = TRACK_MAP_PANEL_ALPHA),
            canvasBg = surfaceVariant.copy(alpha = TRACK_MAP_CANVAS_ALPHA),
            trackSurfaceColor = surfaceVariant.copy(alpha = TRACK_MAP_SURFACE_ALPHA),
            trackEdgeColor = onSurface.copy(alpha = TRACK_MAP_EDGE_ALPHA),
            lineCasingColor = background.copy(alpha = TRACK_MAP_LINE_CASING_ALPHA),
            referenceColor = onSurfaceVariant.copy(alpha = TRACK_MAP_REFERENCE_ALPHA),
            idealColor = cyan.copy(alpha = TRACK_MAP_IDEAL_ALPHA),
            selectedColor = amber.copy(alpha = TRACK_MAP_SELECTED_ALPHA),
            markerRingColor = onSurface.copy(alpha = TRACK_MAP_MARKER_RING_ALPHA),
            markerInnerColor = amber.copy(alpha = TRACK_MAP_SELECTED_ALPHA),
            overlayBg = surface.copy(alpha = TRACK_MAP_OVERLAY_ALPHA),
            overlayFg = onSurface,
            overlayDimAlpha = overlayDimAlpha,
            sectorLabelBg = background.copy(alpha = TRACK_MAP_SECTOR_LABEL_ALPHA),
            sectorSfColor = onSurface,
            sectorS1Color = amber,
            sectorS2Color = cyan,
            sectorOtherColor = teal,
        )
    }
}

internal fun resolveTrackMapViewportPointerTarget(
    pointer: Offset,
    cameraState: SessionAnalysisTrackMapCameraState,
    pointerState: SessionAnalysisTrackMapPointerState,
    purpose: SessionAnalysisTrackMapPointerPurpose,
): Pair<Float?, Long?> = resolveTrackMapPointerTarget(
    context = pointerState,
    pointer = pointer.toCameraBaseSpace(
        anchor = cameraState.anchor,
        focusPoint = cameraState.focusPoint,
        canvasSize = cameraState.canvasSize,
        zoom = cameraState.zoom,
    ),
    purpose = purpose,
)
