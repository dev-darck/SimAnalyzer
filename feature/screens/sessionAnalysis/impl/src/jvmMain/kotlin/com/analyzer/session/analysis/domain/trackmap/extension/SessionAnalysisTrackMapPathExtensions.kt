package com.analyzer.session.analysis.domain.trackmap.extension

import com.analyzer.session.analysis.domain.trackmap.model.PathProjectionMatch
import com.analyzer.session.analysis.domain.trackmap.model.TrackPathPoint
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Path helpers centralize projection and sampling so every track-map tool interprets geometry the same way.
 */
internal fun List<SessionAnalysisTrackMapPoint>.withPathFractions(): List<TrackPathPoint> {
    if (isEmpty()) return emptyList()
    if (size == 1) return listOf(TrackPathPoint(fraction = 0f, point = first()))

    val distances = FloatArray(size)
    var totalDistance = 0f
    for (index in 1 until size) {
        val previous = this[index - 1]
        val current = this[index]
        totalDistance += hypot(current.x - previous.x, current.y - previous.y)
        distances[index] = totalDistance
    }
    val safeTotalDistance = totalDistance.takeIf { it > 0.0001f } ?: return mapIndexed { index, point ->
        val fraction = if (lastIndex <= 0) 0f else index.toFloat() / lastIndex.toFloat()
        TrackPathPoint(fraction = fraction, point = point)
    }
    return mapIndexed { index, point ->
        TrackPathPoint(
            fraction = (distances[index] / safeTotalDistance).coerceIn(0f, 1f),
            point = point,
        )
    }
}

internal fun List<TrackPathPoint>.samplePositionAtPath(fraction: Float): Pair<Float, Float> {
    val (lower, upper, localFraction) = resolvePathSegment(fraction)
    return lerp(lower.point.x, upper.point.x, localFraction) to
        lerp(lower.point.y, upper.point.y, localFraction)
}

internal fun List<TrackPathPoint>.sampleHalfWidthAt(fraction: Float): Float? {
    if (isEmpty()) return null
    val (lower, upper, localFraction) = resolvePathSegment(fraction)
    val lowerWidth = lower.point.halfWidthMeters() ?: return upper.point.halfWidthMeters()
    val upperWidth = upper.point.halfWidthMeters() ?: lowerWidth
    return lerp(lowerWidth, upperWidth, localFraction)
}

internal fun List<TrackPathPoint>.sampleNormalAtPath(fraction: Float): Pair<Float, Float>? {
    if (size < 2) return null
    val clampedFraction = fraction.coerceIn(0f, 1f)
    val centerIndex = indices.minByOrNull { index ->
        abs(this[index].fraction - clampedFraction)
    } ?: return null
    val previousIndex = (centerIndex - 1).coerceAtLeast(0)
    val nextIndex = (centerIndex + 1).coerceAtMost(lastIndex)
    val previous = this[previousIndex].point
    val next = this[nextIndex].point
    val dx = next.x - previous.x
    val dy = next.y - previous.y
    val length = hypot(dx, dy)
    if (!length.isFinite() || length <= 0.0001f) return null
    return -dy / length to dx / length
}

internal fun List<TrackPathPoint>.nearestPathProjectionTo(
    x: Float,
    y: Float,
    preferredFraction: Float?,
    searchWindowFraction: Float,
): PathProjectionMatch? {
    if (size < 2) return null

    val clampedPreferred = preferredFraction?.coerceIn(0f, 1f)
    val lowerBound = clampedPreferred?.minus(searchWindowFraction)?.coerceAtLeast(0f)
    val upperBound = clampedPreferred?.plus(searchWindowFraction)?.coerceAtMost(1f)

    fun resolveMatch(onlyPreferredWindow: Boolean): PathProjectionMatch? {
        var bestMatch: PathProjectionMatch? = null
        for (index in 0 until lastIndex) {
            val start = this[index]
            val end = this[index + 1]
            if (onlyPreferredWindow && lowerBound != null && upperBound != null) {
                val segmentMinFraction = minOf(start.fraction, end.fraction)
                val segmentMaxFraction = maxOf(start.fraction, end.fraction)
                if (segmentMaxFraction < lowerBound || segmentMinFraction > upperBound) continue
            }
            val projection = projectOntoSegment(x = x, y = y, start = start, end = end)
            if (bestMatch == null || projection.distanceSquared < bestMatch.distanceSquared) {
                bestMatch = projection
            }
        }
        return bestMatch
    }

    return resolveMatch(onlyPreferredWindow = clampedPreferred != null)
        ?: resolveMatch(onlyPreferredWindow = false)
}

internal fun List<SessionAnalysisTrackMapPoint>.polylineLengthMeters(): Float {
    if (size < 2) return 0f
    var lengthMeters = 0f
    for (index in 1..lastIndex) {
        val previous = this[index - 1]
        val current = this[index]
        lengthMeters += hypot(current.x - previous.x, current.y - previous.y)
    }
    return lengthMeters
}

internal fun SessionAnalysisTrackMapPoint.halfWidthMeters(): Float? {
    val left = leftWidthMeters?.takeIf { it.isFinite() && it > 0.05f }
    val right = rightWidthMeters?.takeIf { it.isFinite() && it > 0.05f }
    return when {
        left != null && right != null -> (left + right) * 0.5f
        left != null -> left
        right != null -> right
        else -> null
    }
}

internal fun List<SessionAnalysisTrackMapPoint>.samplePositionAt(fraction: Float): Pair<Float, Float> {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    val scaledIndex = clampedFraction * lastIndex.coerceAtLeast(1).toFloat()
    val lowerIndex = scaledIndex.toInt().coerceIn(0, lastIndex)
    val upperIndex = (lowerIndex + 1).coerceAtMost(lastIndex)
    val localFraction = (scaledIndex - lowerIndex.toFloat()).coerceIn(0f, 1f)
    val lower = this[lowerIndex]
    val upper = this[upperIndex]
    return lerp(lower.x, upper.x, localFraction) to lerp(lower.y, upper.y, localFraction)
}

private fun List<TrackPathPoint>.resolvePathSegment(fraction: Float): Triple<TrackPathPoint, TrackPathPoint, Float> {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    val upperIndex = indexOfFirst { pathPoint -> pathPoint.fraction >= clampedFraction }
        .takeIf { index -> index >= 0 }
        ?: lastIndex
    val lowerIndex = (upperIndex - 1).coerceAtLeast(0)
    val lower = this[lowerIndex]
    val upper = this[upperIndex]
    if (lowerIndex == upperIndex) return Triple(lower, upper, 0f)

    val segmentFraction = (upper.fraction - lower.fraction).takeIf { it > 0.0001f } ?: 1f
    val localFraction = ((clampedFraction - lower.fraction) / segmentFraction).coerceIn(0f, 1f)
    return Triple(lower, upper, localFraction)
}

private fun projectOntoSegment(x: Float, y: Float, start: TrackPathPoint, end: TrackPathPoint): PathProjectionMatch {
    val dx = end.point.x - start.point.x
    val dy = end.point.y - start.point.y
    val segmentLengthSquared = dx * dx + dy * dy
    val rawFraction = if (segmentLengthSquared <= 0.0001f) {
        0f
    } else {
        (((x - start.point.x) * dx) + ((y - start.point.y) * dy)) / segmentLengthSquared
    }
    val localFraction = rawFraction.coerceIn(0f, 1f)
    val projectedX = start.point.x + dx * localFraction
    val projectedY = start.point.y + dy * localFraction
    val distanceX = x - projectedX
    val distanceY = y - projectedY
    return PathProjectionMatch(
        fraction = lerp(start.fraction, end.fraction, localFraction),
        distanceSquared = distanceX * distanceX + distanceY * distanceY,
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
