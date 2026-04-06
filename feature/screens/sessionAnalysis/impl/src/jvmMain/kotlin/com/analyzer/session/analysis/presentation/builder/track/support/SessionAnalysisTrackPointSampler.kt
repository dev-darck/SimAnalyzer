package com.analyzer.session.analysis.presentation.builder.track.support

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

internal class SessionAnalysisTrackPointSampler {

    fun <T> sampleEvenly(points: List<T>, maxPoints: Int): List<T> {
        if (points.isEmpty() || maxPoints <= 0) return emptyList()
        if (points.size <= maxPoints) return points
        if (maxPoints == 1) return listOf(points.first())

        val lastSourceIndex = points.lastIndex
        return buildList(maxPoints) {
            repeat(maxPoints) { sampleIndex ->
                val fraction = sampleIndex.toFloat() / (maxPoints - 1).toFloat()
                val sourceIndex = (fraction * lastSourceIndex.toFloat()).roundToInt().coerceIn(0, lastSourceIndex)
                add(points[sourceIndex])
            }
        }
    }

    fun <T> sampleEvenlyByPathDistance(
        points: List<T>,
        maxPoints: Int,
        xSelector: (T) -> Float,
        ySelector: (T) -> Float,
    ): List<T> {
        if (points.isEmpty() || maxPoints <= 0) return emptyList()
        if (points.size <= maxPoints) return points
        if (maxPoints == 1) return listOf(points.first())

        val cumulativeDistances = FloatArray(points.size)
        var totalDistance = 0f
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            val segmentLength = hypot(
                xSelector(current) - xSelector(previous),
                ySelector(current) - ySelector(previous),
            )
            if (segmentLength.isFinite() && segmentLength > 0.0001f) {
                totalDistance += segmentLength
            }
            cumulativeDistances[index] = totalDistance
        }
        if (totalDistance <= 0.0001f) {
            return sampleEvenly(points = points, maxPoints = maxPoints)
        }

        val selected = ArrayList<T>(maxPoints)
        val lastSourceIndex = points.lastIndex
        var minimumIndex = 0
        repeat(maxPoints) { sampleIndex ->
            val remainingSamples = maxPoints - sampleIndex
            val maximumIndex = lastSourceIndex - (remainingSamples - 1)
            val targetDistance = totalDistance * sampleIndex.toFloat() / (maxPoints - 1).toFloat()
            val sourceIndex = closestDistanceIndex(
                distances = cumulativeDistances,
                targetDistance = targetDistance,
                startIndex = minimumIndex,
                endIndex = maximumIndex,
            )
            selected += points[sourceIndex]
            minimumIndex = sourceIndex + 1
        }
        return selected
    }

    fun median(values: List<Float>): Float? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middleIndex = sorted.lastIndex / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middleIndex] + sorted[middleIndex + 1]) * 0.5f
        } else {
            sorted[middleIndex]
        }
    }

    private fun closestDistanceIndex(
        distances: FloatArray,
        targetDistance: Float,
        startIndex: Int,
        endIndex: Int,
    ): Int {
        if (startIndex >= endIndex) return startIndex.coerceIn(0, distances.lastIndex)

        val insertionPoint = distances.binarySearch(
            element = targetDistance,
            fromIndex = startIndex,
            toIndex = endIndex + 1,
        )
        if (insertionPoint >= 0) return insertionPoint

        val upperIndex = (-insertionPoint - 1).coerceIn(startIndex, endIndex + 1)
        val lowerIndex = (upperIndex - 1).coerceIn(startIndex, endIndex)
        if (upperIndex > endIndex) return lowerIndex

        val lowerDistance = abs(distances[lowerIndex] - targetDistance)
        val upperDistance = abs(distances[upperIndex] - targetDistance)
        return if (upperDistance < lowerDistance) upperIndex else lowerIndex
    }
}
