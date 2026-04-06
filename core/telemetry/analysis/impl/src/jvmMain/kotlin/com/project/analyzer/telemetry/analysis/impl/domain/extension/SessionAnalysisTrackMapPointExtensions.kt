package com.project.analyzer.telemetry.analysis.impl.domain.extension

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlin.math.hypot

/**
 * Track-point geometry helpers keep map assembly and zone detection on the same coordinate rules.
 */
internal fun List<SessionAnalysisTrackMapPoint>.cumulativeDistances(): List<Float> {
    if (isEmpty()) return emptyList()
    val result = MutableList(size) { 0f }
    var total = 0f
    for (index in 1 until size) {
        val previous = this[index - 1]
        val current = this[index]
        val segmentLength = hypot(current.x - previous.x, current.y - previous.y)
        if (segmentLength.isFinite() && segmentLength > 0.0001f) {
            total += segmentLength
        }
        result[index] = total
    }
    return result
}

internal fun List<SessionAnalysisTrackMapPoint>.nearestIndexToPoint(
    x: Float,
    y: Float,
    startIndex: Int = 0,
    endIndex: Int = lastIndex,
): Int {
    if (isEmpty()) return 0
    var bestIndex = startIndex.coerceIn(0, lastIndex)
    var bestDistance = Float.POSITIVE_INFINITY
    val clampedStart = startIndex.coerceIn(0, lastIndex)
    val clampedEnd = endIndex.coerceIn(clampedStart, lastIndex)
    for (index in clampedStart..clampedEnd) {
        val point = this[index]
        val dx = point.x - x
        val dy = point.y - y
        val distance = dx * dx + dy * dy
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex
}
