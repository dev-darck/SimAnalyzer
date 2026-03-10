package com.analyzer.trackmap.builder.recording.lap

import com.analyzer.trackmap.builder.recording.geometry.findClosestIndex
import com.project.analyzer.math.Vec2

internal class TrackMapMerger(
    private val insertDistanceMeters: Float,
    private val minInsertSpacingMeters: Float,
    private val searchWindow: Int,
) {

    fun merge(mapPoints: MutableList<Vec2>, lap: List<Vec2>) {
        if (lap.isEmpty()) return
        if (mapPoints.isEmpty()) {
            mapPoints.addAll(lap)
            return
        }

        var index = mapPoints.findClosestIndex(lap.first())
        var lastInserted: Vec2? = null
        for (point in lap) {
            index = findNearestForwardIndex(mapPoints, point, index)
            val dist = mapPoints[index].distanceTo(point)
            if (dist <= insertDistanceMeters) continue

            val canInsert = lastInserted?.distanceTo(point)?.let {
                it >= minInsertSpacingMeters
            } ?: true
            if (!canInsert) continue

            val insertIndex = (index + 1).coerceAtMost(mapPoints.size)
            mapPoints.add(insertIndex, point)
            lastInserted = point
            index = insertIndex
        }
    }

    private fun findNearestForwardIndex(points: List<Vec2>, target: Vec2, startIndex: Int): Int {
        val count = points.size
        if (count == 0) return 0
        val safeStart = startIndex.coerceIn(0, count - 1)
        var bestIdx = safeStart
        var bestDist = points[safeStart].distanceTo(target)
        val window = searchWindow.coerceAtMost(count)
        for (offset in 1..window) {
            val idx = (safeStart + offset) % count
            val dist = points[idx].distanceTo(target)
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = idx
            }
        }
        return bestIdx
    }
}
