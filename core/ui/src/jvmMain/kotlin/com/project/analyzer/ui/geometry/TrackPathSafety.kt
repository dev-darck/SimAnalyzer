package com.project.analyzer.ui.geometry

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TRACK_PATH_INTERSECTION_EPSILON = 1e-4f
private const val TRACK_PATH_SEGMENT_LENGTH_EPSILON = 1e-4f

public fun List<Offset>.hasTrackPathSelfIntersection(closed: Boolean = false, maxSamplePoints: Int = 480): Boolean {
    val sampledPoints = sampleTrackPathPoints(maxSamplePoints)
    if (sampledPoints.size < 4) return false

    val segmentCount = if (closed) sampledPoints.size else sampledPoints.size - 1

    for (firstIndex in 0 until segmentCount) {
        val firstStart = sampledPoints[firstIndex]
        val firstEnd = sampledPoints[(firstIndex + 1) % sampledPoints.size]
        if (firstStart.isDegenerateTo(firstEnd)) continue

        for (secondIndex in firstIndex + 1 until segmentCount) {
            if (segmentIndicesAreAdjacent(firstIndex, secondIndex, segmentCount, closed)) continue

            val secondStart = sampledPoints[secondIndex]
            val secondEnd = sampledPoints[(secondIndex + 1) % sampledPoints.size]
            if (secondStart.isDegenerateTo(secondEnd)) continue
            if (!boundingBoxesOverlap(firstStart, firstEnd, secondStart, secondEnd)) continue
            if (segmentsIntersect(firstStart, firstEnd, secondStart, secondEnd)) return true
        }
    }

    return false
}

private fun List<Offset>.sampleTrackPathPoints(maxSamplePoints: Int): List<Offset> {
    if (maxSamplePoints !in 2..<size) return this

    val sampled = ArrayList<Offset>(maxSamplePoints)
    val lastSourceIndex = lastIndex
    val lastSampleIndex = maxSamplePoints - 1

    for (sampleIndex in 0..lastSampleIndex) {
        val sourceIndex = ((sampleIndex.toLong() * lastSourceIndex.toLong()) / lastSampleIndex.toLong())
            .toInt()
            .coerceIn(0, lastSourceIndex)
        sampled += this[sourceIndex]
    }

    return sampled
}

private fun segmentIndicesAreAdjacent(firstIndex: Int, secondIndex: Int, segmentCount: Int, closed: Boolean): Boolean {
    if (secondIndex - firstIndex <= 1) return true
    return closed && firstIndex == 0 && secondIndex == segmentCount - 1
}

private fun boundingBoxesOverlap(
    firstStart: Offset,
    firstEnd: Offset,
    secondStart: Offset,
    secondEnd: Offset,
): Boolean {
    val firstMinX = min(firstStart.x, firstEnd.x)
    val firstMaxX = max(firstStart.x, firstEnd.x)
    val firstMinY = min(firstStart.y, firstEnd.y)
    val firstMaxY = max(firstStart.y, firstEnd.y)
    val secondMinX = min(secondStart.x, secondEnd.x)
    val secondMaxX = max(secondStart.x, secondEnd.x)
    val secondMinY = min(secondStart.y, secondEnd.y)
    val secondMaxY = max(secondStart.y, secondEnd.y)

    return firstMaxX >= secondMinX - TRACK_PATH_INTERSECTION_EPSILON &&
        secondMaxX >= firstMinX - TRACK_PATH_INTERSECTION_EPSILON &&
        firstMaxY >= secondMinY - TRACK_PATH_INTERSECTION_EPSILON &&
        secondMaxY >= firstMinY - TRACK_PATH_INTERSECTION_EPSILON
}

private fun segmentsIntersect(firstStart: Offset, firstEnd: Offset, secondStart: Offset, secondEnd: Offset): Boolean {
    val rx = firstEnd.x - firstStart.x
    val ry = firstEnd.y - firstStart.y
    val sx = secondEnd.x - secondStart.x
    val sy = secondEnd.y - secondStart.y
    val cross = cross(rx, ry, sx, sy)
    if (abs(cross) <= TRACK_PATH_INTERSECTION_EPSILON) return false

    val qpx = secondStart.x - firstStart.x
    val qpy = secondStart.y - firstStart.y
    val t = cross(qpx, qpy, sx, sy) / cross
    val u = cross(qpx, qpy, rx, ry) / cross

    return t > TRACK_PATH_INTERSECTION_EPSILON &&
        t < 1f - TRACK_PATH_INTERSECTION_EPSILON &&
        u > TRACK_PATH_INTERSECTION_EPSILON &&
        u < 1f - TRACK_PATH_INTERSECTION_EPSILON
}

private fun cross(ax: Float, ay: Float, bx: Float, by: Float): Float = ax * by - ay * bx

private fun Offset.isDegenerateTo(other: Offset): Boolean {
    val dx = x - other.x
    val dy = y - other.y
    return dx * dx + dy * dy <= TRACK_PATH_SEGMENT_LENGTH_EPSILON * TRACK_PATH_SEGMENT_LENGTH_EPSILON
}
