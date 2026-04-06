package com.analyzer.trackmap.presentation.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationViewport

@Immutable
internal data class TrackMapCalibrationCanvasPalette(
    val surfaceColor: Color,
    val baseTrackGlow: Color,
    val baseTrackStroke: Color,
    val centerLine: Color,
    val sectorHighlight: Color,
    val liveHalo: Color,
    val liveMarker: Color,
)

internal fun DrawScope.drawTrackMapCalibrationCanvasScene(
    scene: TrackMapCalibrationEditorCanvasScene,
    palette: TrackMapCalibrationCanvasPalette,
) {
    drawTrackBase(
        path = scene.trackBasePath,
        glowColor = palette.baseTrackGlow,
        strokeColor = palette.baseTrackStroke,
        dashColor = palette.centerLine,
    )
    scene.selectedSector?.let { sector ->
        val path = scene.sectorPathsByEndGateId[sector.endGateId] ?: return@let
        drawSelectedSector(
            path = path,
            color = palette.sectorHighlight,
            alpha = 0.22f,
        )
    }
    scene.markerGeometries.values.forEach { geometry ->
        val effectiveGeometry = scene.selectedMarkerGeometry
            ?.takeIf { it.gateId == geometry.gateId }
            ?: geometry
        if (effectiveGeometry.gateId == scene.selectedMarker?.gateId) {
            drawGateGuide(
                geometry = effectiveGeometry,
                viewport = scene.viewport,
                selected = true,
            )
        }
        drawMarkerPoint(
            center = scene.viewport.worldToScreen(effectiveGeometry.center),
            color = effectiveGeometry.color,
            selected = effectiveGeometry.gateId == scene.selectedMarker?.gateId ||
                effectiveGeometry.gateId == scene.selectedSector?.startGateId,
            surfaceColor = palette.surfaceColor,
        )
    }
    scene.previewAnchor?.let { anchor ->
        drawPreviewAnchor(
            center = scene.viewport.worldToScreen(anchor.center),
            color = scene.selectedMarker
                ?.let { Color(it.colorHex) }
                ?: palette.centerLine,
        )
    }
    scene.livePositionOnScreen?.let { position ->
        drawLiveCarMarker(
            center = position,
            haloColor = palette.liveHalo,
            markerColor = palette.liveMarker,
        )
    }
}

private fun DrawScope.drawLiveCarMarker(center: Offset, haloColor: Color, markerColor: Color) {
    drawCircle(
        color = haloColor,
        radius = 16f,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = 6f,
        center = center,
    )
    drawCircle(
        color = markerColor,
        radius = 3.5f,
        center = center,
    )
}

private fun DrawScope.drawPreviewAnchor(center: Offset, color: Color) {
    drawCircle(
        color = color.copy(alpha = 0.18f),
        radius = 16f,
        center = center,
    )
    drawCircle(
        color = color,
        radius = 7f,
        center = center,
    )
}

private fun DrawScope.drawGateGuide(
    geometry: TrackMapCalibrationMarkerGeometry,
    viewport: TrackMapCalibrationViewport,
    selected: Boolean,
) {
    val lineStart = viewport.worldToScreen(
        geometry.center - geometry.normal * geometry.halfWidthMeters,
    )
    val lineEnd = viewport.worldToScreen(
        geometry.center + geometry.normal * geometry.halfWidthMeters,
    )
    val center = viewport.worldToScreen(geometry.center)
    val arrowTip = viewport.worldToScreen(geometry.directionHandlePoint)
    val strokeWidth = if (selected) 4.5f else 2.5f
    val alpha = if (selected) 0.92f else 0.34f
    drawLine(
        color = geometry.color.copy(alpha = alpha),
        start = lineStart,
        end = lineEnd,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = geometry.color.copy(alpha = alpha),
        start = center,
        end = arrowTip,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    val arrowDirection = (arrowTip - center).normalizedSafe()
    val leftWing = rotateOffset(arrowDirection, 2.55f) * 10f
    val rightWing = rotateOffset(arrowDirection, -2.55f) * 10f
    drawLine(
        color = geometry.color.copy(alpha = alpha),
        start = arrowTip,
        end = arrowTip + leftWing,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = geometry.color.copy(alpha = alpha),
        start = arrowTip,
        end = arrowTip + rightWing,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawTrackBase(path: Path, glowColor: Color, strokeColor: Color, dashColor: Color) {
    drawPath(
        path = path,
        color = glowColor,
        style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = dashColor,
        style = Stroke(
            width = 1.8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 11f)),
        ),
    )
}

private fun DrawScope.drawSelectedSector(path: Path, color: Color, alpha: Float) {
    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawMarkerPoint(center: Offset, color: Color, selected: Boolean, surfaceColor: Color) {
    val radius = if (selected) 7f else 4.5f
    drawCircle(
        color = color.copy(alpha = if (selected) 0.24f else 0.18f),
        radius = radius + if (selected) 6f else 4f,
        center = center,
    )
    drawCircle(
        color = surfaceColor,
        radius = radius + 1.5f,
        center = center,
    )
    drawCircle(
        color = color,
        radius = radius,
        center = center,
    )
}

private fun Offset.normalizedSafe(): Offset {
    val length = kotlin.math.sqrt(x * x + y * y)
    if (length <= 0.0001f) return Offset(1f, 0f)
    return Offset(x / length, y / length)
}

private fun rotateOffset(offset: Offset, radians: Float): Offset {
    val sin = kotlin.math.sin(radians)
    val cos = kotlin.math.cos(radians)
    return Offset(
        x = offset.x * cos - offset.y * sin,
        y = offset.x * sin + offset.y * cos,
    )
}
