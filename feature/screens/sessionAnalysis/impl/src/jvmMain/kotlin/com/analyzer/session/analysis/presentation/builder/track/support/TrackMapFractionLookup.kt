package com.analyzer.session.analysis.presentation.builder.track.support

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlin.math.hypot

/**
 * Provides fast projection and interpolation helpers over a track polyline expressed in normalized
 * lap fractions.
 */
internal class TrackMapFractionLookup private constructor(
    val points: List<SessionAnalysisTrackMapPoint>,
    private val fractions: FloatArray,
) {

    fun samplePositionAt(fraction: Float): Pair<Float, Float>? {
        if (points.size < 2) return null
        val safeFraction = fraction.coerceIn(0f, 1f)
        val upperIndex = indexAtOrAfter(safeFraction)
        if (upperIndex <= 0) {
            val first = points.first()
            return first.x to first.y
        }
        val lowerIndex = upperIndex - 1
        val lowerFraction = fractions[lowerIndex]
        val upperFraction = fractions[upperIndex]
        val localFraction = (
            (safeFraction - lowerFraction) /
                (upperFraction - lowerFraction).takeIf { it > 0.0001f }.orOne()
            ).coerceIn(0f, 1f)
        val lower = points[lowerIndex]
        val upper = points[upperIndex]
        return interpolate(
            start = lower.x,
            stop = upper.x,
            fraction = localFraction,
        ) to interpolate(
            start = lower.y,
            stop = upper.y,
            fraction = localFraction,
        )
    }

    fun sampleNormalAt(fraction: Float): Pair<Float, Float>? {
        val safeFraction = fraction.coerceIn(0f, 1f)
        val previous = samplePositionAt((safeFraction - 0.0025f).coerceAtLeast(0f)) ?: return null
        val next = samplePositionAt((safeFraction + 0.0025f).coerceAtMost(1f)) ?: return null
        val dx = next.first - previous.first
        val dy = next.second - previous.second
        val length = hypot(dx, dy)
        if (!length.isFinite() || length <= 0.0001f) return null
        return -dy / length to dx / length
    }

    /**
     * Projects a world-space point onto the closest track segment, optionally constraining the search
     * around a preferred fraction to reduce false matches on overlapping parts of the circuit.
     */
    fun nearestProjectionTo(
        x: Float,
        y: Float,
        preferredFraction: Float?,
        fractionWindow: Float?,
    ): TrackProjectionMatch? {
        var bestMatch: TrackProjectionMatch? = null
        val lowerBound = preferredFraction?.let { fraction ->
            fractionWindow?.let { window -> (fraction - window).coerceAtLeast(0f) }
        }
        val upperBound = preferredFraction?.let { fraction ->
            fractionWindow?.let { window -> (fraction + window).coerceAtMost(1f) }
        }

        for (index in 1..points.lastIndex) {
            val startFraction = fractions[index - 1]
            val endFraction = fractions[index]
            if (lowerBound != null && upperBound != null) {
                val segmentMinFraction = minOf(startFraction, endFraction)
                val segmentMaxFraction = maxOf(startFraction, endFraction)
                if (segmentMaxFraction < lowerBound || segmentMinFraction > upperBound) continue
            }

            val startPoint = points[index - 1]
            val endPoint = points[index]
            val segmentX = endPoint.x - startPoint.x
            val segmentY = endPoint.y - startPoint.y
            val segmentLengthSquared = segmentX * segmentX + segmentY * segmentY
            val localFraction = if (segmentLengthSquared <= 0.0001f) {
                0f
            } else {
                (((x - startPoint.x) * segmentX) + ((y - startPoint.y) * segmentY)) / segmentLengthSquared
            }.coerceIn(0f, 1f)
            val projectedX = interpolate(startPoint.x, endPoint.x, localFraction)
            val projectedY = interpolate(startPoint.y, endPoint.y, localFraction)
            val deltaX = x - projectedX
            val deltaY = y - projectedY
            val distanceSquared = deltaX * deltaX + deltaY * deltaY
            if (bestMatch == null || distanceSquared < bestMatch.distanceSquared) {
                bestMatch = TrackProjectionMatch(
                    fraction = interpolate(startFraction, endFraction, localFraction),
                    distanceSquared = distanceSquared,
                )
            }
        }

        return bestMatch
    }

    /**
     * Finds the index of the first element in the [fractions] list that is greater than or equal to the specified [fraction].
     *
     * @param fraction The threshold value to compare against the elements in [fractions].
     * @return The index of the first element in [fractions] that is not less than [fraction], or the size of the list if no such element is found.
     */
    private fun indexAtOrAfter(fraction: Float): Int {
        var low = 0
        var high = fractions.lastIndex
        while (low < high) {
            val mid = (low + high) ushr 1
            if (fractions[mid] < fraction) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low
    }

    private fun Float?.orOne(): Float = this ?: 1f

    private fun interpolate(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

    companion object {

        fun create(
            points: List<SessionAnalysisTrackMapPoint>,
            fractionsOverride: List<Float>? = null,
        ): TrackMapFractionLookup? {
            if (points.size < 2) return null
            fractionsOverride
                ?.takeIf { fractions -> fractions.size == points.size }
                ?.toMonotonicFractionArray()
                ?.let { fractions -> return TrackMapFractionLookup(points = points, fractions = fractions) }

            val fractions = FloatArray(points.size)
            var totalDistance = 0f
            for (index in 1 until points.size) {
                totalDistance += hypot(
                    points[index].x - points[index - 1].x,
                    points[index].y - points[index - 1].y,
                )
                fractions[index] = totalDistance
            }
            if (!totalDistance.isFinite() || totalDistance <= 0.0001f) {
                for (index in fractions.indices) {
                    fractions[index] = if (points.lastIndex <= 0) 0f else index.toFloat() / points.lastIndex.toFloat()
                }
            } else {
                for (index in fractions.indices) {
                    fractions[index] = (fractions[index] / totalDistance).coerceIn(0f, 1f)
                }
            }
            fractions[0] = 0f
            fractions[fractions.lastIndex] = 1f
            return TrackMapFractionLookup(points = points, fractions = fractions)
        }

        private fun List<Float>.toMonotonicFractionArray(): FloatArray? {
            val result = FloatArray(size)
            var previous = 0f
            forEachIndexed { index, value ->
                if (!value.isFinite()) return null
                val fraction = value.coerceIn(0f, 1f)
                if (index > 0 && fraction < previous - 0.0001f) return null
                result[index] = fraction.coerceAtLeast(previous)
                previous = result[index]
            }
            return result.takeIf { fractions -> fractions.last() - fractions.first() > 0.0001f }
        }
    }
}
