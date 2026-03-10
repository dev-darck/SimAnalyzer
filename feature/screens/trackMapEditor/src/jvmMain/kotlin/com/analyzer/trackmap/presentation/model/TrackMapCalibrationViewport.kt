package com.analyzer.trackmap.presentation.model

import androidx.compose.ui.geometry.Offset
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import kotlin.math.min

data class TrackMapCalibrationViewport(val scale: Float, val offsetX: Float, val offsetY: Float) {

    fun worldToScreen(point: Vec2): Offset = Offset(
        x = point.x * scale + offsetX,
        y = point.y * scale + offsetY,
    )

    fun screenToWorld(point: Offset): Vec2 = Vec2(
        x = (point.x - offsetX) / scale,
        y = (point.y - offsetY) / scale,
    )

    companion object {

        fun fit(
            bounds: TrackMapBounds,
            widthPx: Float,
            heightPx: Float,
            paddingPx: Float,
        ): TrackMapCalibrationViewport {
            val widthMeters = (bounds.maxX - bounds.minX).coerceAtLeast(1f)
            val heightMeters = (bounds.maxY - bounds.minY).coerceAtLeast(1f)
            val scale = min(
                (widthPx - paddingPx * 2f) / widthMeters,
                (heightPx - paddingPx * 2f) / heightMeters,
            ).coerceAtLeast(0.0001f)
            val offsetX = (widthPx - widthMeters * scale) * 0.5f - bounds.minX * scale
            val offsetY = (heightPx - heightMeters * scale) * 0.5f - bounds.minY * scale
            return TrackMapCalibrationViewport(
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
            )
        }
    }
}
