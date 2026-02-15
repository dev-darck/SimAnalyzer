package com.project.analyzer.calibration.trackmap

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import kotlin.math.max
import kotlin.math.min

class TrackMapStatsCalculator {

    fun computeDistance(points: List<Vec2>): Float {
        if (points.size < 2) return 0f
        var sum = 0f
        for (i in 1 until points.size) {
            sum += points[i - 1].distanceTo(points[i])
        }
        return sum
    }

    fun computeBounds(points: List<Vec2>): TrackMapBounds? {
        if (points.isEmpty()) return null
        var minX = points[0].x
        var minY = points[0].y
        var maxX = points[0].x
        var maxY = points[0].y

        for (point in points) {
            minX = min(minX, point.x)
            minY = min(minY, point.y)
            maxX = max(maxX, point.x)
            maxY = max(maxY, point.y)
        }

        return TrackMapBounds(
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY
        )
    }

    fun updateBounds(current: TrackMapBounds?, point: Vec2): TrackMapBounds {
        val minX = current?.minX ?: point.x
        val minY = current?.minY ?: point.y
        val maxX = current?.maxX ?: point.x
        val maxY = current?.maxY ?: point.y

        return TrackMapBounds(
            minX = min(minX, point.x),
            minY = min(minY, point.y),
            maxX = max(maxX, point.x),
            maxY = max(maxY, point.y),
        )
    }
}
