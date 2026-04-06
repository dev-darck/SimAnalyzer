package com.analyzer.session.analysis.presentation.builder.track.support

import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlin.math.hypot

internal class SessionAnalysisTrackPolylineSmoother(
    private val pointSampler: SessionAnalysisTrackPointSampler = SessionAnalysisTrackPointSampler(),
) {

    fun smooth(
        points: List<SessionAnalysisFractionPointUi>,
        closed: Boolean,
        passes: Int = 1,
    ): List<SessionAnalysisFractionPointUi> {
        if (points.size < 5 || passes <= 0) return points

        val maxLocalGap = resolveMaxLocalGap(points) ?: return points
        var current = points
        repeat(passes) {
            current = smoothPass(
                points = current,
                closed = closed,
                maxLocalGap = maxLocalGap,
            )
        }
        return current
    }

    private fun smoothPass(
        points: List<SessionAnalysisFractionPointUi>,
        closed: Boolean,
        maxLocalGap: Float,
    ): List<SessionAnalysisFractionPointUi> {
        val result = ArrayList<SessionAnalysisFractionPointUi>(points.size)
        val lastIndex = points.lastIndex

        points.indices.forEach { index ->
            if (!closed && (index == 0 || index == lastIndex)) {
                result += points[index]
                return@forEach
            }
            if (hasLocalGap(points = points, index = index, closed = closed, maxLocalGap = maxLocalGap)) {
                result += points[index]
                return@forEach
            }

            var weightedX = 0f
            var weightedY = 0f
            var totalWeight = 0f
            for (offset in -SmoothingRadius..SmoothingRadius) {
                val sourceIndex = resolveIndex(
                    index = index + offset,
                    lastIndex = lastIndex,
                    closed = closed,
                ) ?: continue
                val weight = SmoothingWeights[offset + SmoothingRadius]
                val point = points[sourceIndex]
                weightedX += point.x * weight
                weightedY += point.y * weight
                totalWeight += weight
            }

            result += if (totalWeight > 0f) {
                points[index].copy(
                    x = weightedX / totalWeight,
                    y = weightedY / totalWeight,
                )
            } else {
                points[index]
            }
        }
        return result
    }

    private fun hasLocalGap(
        points: List<SessionAnalysisFractionPointUi>,
        index: Int,
        closed: Boolean,
        maxLocalGap: Float,
    ): Boolean {
        val lastIndex = points.lastIndex
        for (offset in -SmoothingRadius until SmoothingRadius) {
            val startIndex = resolveIndex(
                index = index + offset,
                lastIndex = lastIndex,
                closed = closed,
            ) ?: continue
            val endIndex = resolveIndex(
                index = index + offset + 1,
                lastIndex = lastIndex,
                closed = closed,
            ) ?: continue
            if (distanceBetween(points[startIndex], points[endIndex]) > maxLocalGap) {
                return true
            }
        }
        return false
    }

    private fun resolveMaxLocalGap(points: List<SessionAnalysisFractionPointUi>): Float? {
        val segmentLengths = buildList {
            for (index in 1 until points.size) {
                val segmentLength = distanceBetween(points[index - 1], points[index])
                if (segmentLength.isFinite() && segmentLength > 0.0001f) add(segmentLength)
            }
        }
        val medianSegmentLength = pointSampler.median(segmentLengths) ?: return null
        return maxOf(MinimumLocalGap, medianSegmentLength * LocalGapMultiplier)
    }

    private fun resolveIndex(index: Int, lastIndex: Int, closed: Boolean): Int? {
        if (closed) {
            val size = lastIndex + 1
            var wrapped = index % size
            if (wrapped < 0) wrapped += size
            return wrapped
        }
        return index.takeIf { candidate -> candidate in 0..lastIndex }
    }

    private fun distanceBetween(first: SessionAnalysisFractionPointUi, second: SessionAnalysisFractionPointUi): Float =
        hypot(second.x - first.x, second.y - first.y)

    private companion object {

        private const val SmoothingRadius: Int = 2
        private val SmoothingWeights = floatArrayOf(1f, 2f, 4f, 2f, 1f)
        private const val LocalGapMultiplier: Float = 4.5f
        private const val MinimumLocalGap: Float = 0.75f
    }
}
