package com.analyzer.session.analysis.presentation.components.map.draw

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPalette
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapSurfaceDrawState
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapTelemetryDrawState
import com.analyzer.session.analysis.presentation.components.map.model.sectorAccent
import com.analyzer.session.analysis.presentation.components.map.support.toOffset

internal fun DrawScope.drawTrackMapScene(
    surfaceState: SessionAnalysisTrackMapSurfaceDrawState,
    telemetryState: SessionAnalysisTrackMapTelemetryDrawState,
    palette: SessionAnalysisTrackMapPalette,
) {
    drawRect(color = palette.canvasBg)

    drawTrackSurface(surfaceState = surfaceState)

    val clipSurfacePath = surfaceState.trackSurfacePath.takeIf { surfaceState.clipTelemetryToSurface }
    if (clipSurfacePath != null) {
        clipPath(clipSurfacePath) {
            drawTrackSectorBoundaries(
                telemetryState = telemetryState,
                palette = palette,
            )
            drawTrackReferenceTrace(
                telemetryState = telemetryState,
                palette = palette,
            )
            drawIdealTrace(
                telemetryState = telemetryState,
                palette = palette,
            )
        }
    } else {
        drawTrackSectorBoundaries(
            telemetryState = telemetryState,
            palette = palette,
        )
        drawTrackReferenceTrace(
            telemetryState = telemetryState,
            palette = palette,
        )
        drawIdealTrace(
            telemetryState = telemetryState,
            palette = palette,
        )
    }

    drawSelectedTelemetry(
        telemetryState = telemetryState,
        palette = palette,
    )
    drawTrackActiveMarker(
        telemetryState = telemetryState,
        palette = palette,
    )
}

private fun DrawScope.drawTrackSurface(surfaceState: SessionAnalysisTrackMapSurfaceDrawState) {
    if (surfaceState.centerLineInView.size < 2) return
    val strokeScale = surfaceState.strokeScale
    val baseSurfaceCasingColor = surfaceState.trackEdgeColor.copy(alpha = 0.22f)
    val baseSurfaceColor = surfaceState.trackSurfaceColor.copy(
        alpha = surfaceState.trackSurfaceColor.alpha.coerceAtLeast(0.74f),
    )
    drawPath(
        path = surfaceState.trackSurfaceBasePath,
        color = baseSurfaceCasingColor,
        style = Stroke(
            width = (surfaceState.surfaceStrokePx + 3f) * strokeScale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
    drawPath(
        path = surfaceState.trackSurfaceBasePath,
        color = baseSurfaceColor,
        style = Stroke(
            width = surfaceState.surfaceStrokePx * strokeScale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun DrawScope.drawTrackSectorBoundaries(
    telemetryState: SessionAnalysisTrackMapTelemetryDrawState,
    palette: SessionAnalysisTrackMapPalette,
) {
    val strokeWidths = telemetryState.strokeWidths
    telemetryState.sectorBoundariesInView.forEach { boundary ->
        val sectorAccent = palette.sectorAccent(boundary.label)
        drawLine(
            color = palette.lineCasingColor.copy(alpha = 0.26f),
            start = boundary.start,
            end = boundary.end,
            strokeWidth = strokeWidths.sectorBoundaryCasing,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = sectorAccent.copy(alpha = 0.42f * palette.overlayDimAlpha),
            start = boundary.start,
            end = boundary.end,
            strokeWidth = strokeWidths.sectorBoundaryCore,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawTrackReferenceTrace(
    telemetryState: SessionAnalysisTrackMapTelemetryDrawState,
    palette: SessionAnalysisTrackMapPalette,
) {
    val strokeWidths = telemetryState.strokeWidths
    if (telemetryState.referenceTraceInView.size >= 2) {
        drawPath(
            path = telemetryState.referenceLinePath,
            color = palette.lineCasingColor.copy(alpha = 0.26f * palette.overlayDimAlpha),
            style = Stroke(
                width = strokeWidths.referenceCasing,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawPath(
            path = telemetryState.referenceLinePath,
            color = palette.referenceColor.copy(alpha = palette.overlayDimAlpha),
            style = Stroke(
                width = strokeWidths.referenceCore,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private fun DrawScope.drawIdealTrace(
    telemetryState: SessionAnalysisTrackMapTelemetryDrawState,
    palette: SessionAnalysisTrackMapPalette,
) {
    val strokeWidths = telemetryState.strokeWidths
    if (telemetryState.idealLineInView.size >= 2) {
        drawPath(
            path = telemetryState.idealLinePath,
            color = palette.lineCasingColor.copy(alpha = 0.78f * palette.overlayDimAlpha.coerceAtLeast(0.9f)),
            style = Stroke(
                width = strokeWidths.idealCasing,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawPath(
            path = telemetryState.idealLinePath,
            color = palette.idealColor.copy(alpha = palette.overlayDimAlpha.coerceAtLeast(0.94f)),
            style = Stroke(
                width = strokeWidths.idealCore,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private fun DrawScope.drawSelectedTelemetry(
    telemetryState: SessionAnalysisTrackMapTelemetryDrawState,
    palette: SessionAnalysisTrackMapPalette,
) {
    val strokeWidths = telemetryState.strokeWidths
    if (telemetryState.selectedTraceInView.size >= 2) {
        drawPath(
            path = telemetryState.selectedBasePath,
            color = palette.selectedColor.copy(alpha = 0.12f),
            style = Stroke(
                width = strokeWidths.selectedBase,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
    if (telemetryState.selectedTrailTraceInView.size >= 2) {
        drawPath(
            path = telemetryState.selectedTrailPath,
            color = palette.lineCasingColor.copy(alpha = 0.84f),
            style = Stroke(
                width = strokeWidths.selectedTrailCasing,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawPath(
            path = telemetryState.selectedTrailPath,
            color = palette.selectedColor.copy(alpha = 0.98f),
            style = Stroke(
                width = strokeWidths.selectedTrailCore,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private fun DrawScope.drawTrackActiveMarker(
    telemetryState: SessionAnalysisTrackMapTelemetryDrawState,
    palette: SessionAnalysisTrackMapPalette,
) {
    val marker = telemetryState.activeMarkerInView ?: return
    val strokeWidths = telemetryState.strokeWidths
    val markerOffset = marker.toOffset()
    drawCircle(
        color = palette.markerRingColor,
        radius = strokeWidths.markerRingRadius,
        center = markerOffset,
        style = Stroke(width = strokeWidths.markerRingStroke),
    )
    drawCircle(
        color = palette.markerInnerColor,
        radius = strokeWidths.markerInnerRadius,
        center = markerOffset,
    )
    telemetryState.displayMarkerDirection?.let { direction ->
        drawLine(
            color = palette.markerRingColor,
            start = markerOffset,
            end = markerOffset + direction * strokeWidths.markerDirectionLength,
            strokeWidth = strokeWidths.markerRingStroke,
            cap = StrokeCap.Round,
        )
    }
}
