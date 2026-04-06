package com.analyzer.session.analysis.presentation.components.map.path

import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.components.map.support.medianOrNull
import kotlin.math.hypot

private const val segmentedTrackPathGapMultiplier: Float = 6f
private const val segmentedTrackPathPointEpsilon: Float = 0.001f

internal fun segmentTrackPath(
    points: List<Offset>,
    closed: Boolean,
    minimumGapPx: Float = 0f,
    gapMultiplier: Float = segmentedTrackPathGapMultiplier,
    breakIndices: Set<Int> = emptySet(),
): SegmentedTrackPath {
    if (points.isEmpty()) {
        return SegmentedTrackPath(
            segments = emptyList(),
            closeLoop = false,
        )
    }
    if (points.size == 1) {
        return SegmentedTrackPath(
            segments = listOf(points),
            closeLoop = false,
        )
    }

    val jumpThreshold = points.computeSegmentedTrackPathJumpThreshold(
        gapMultiplier = gapMultiplier,
        minimumGapPx = minimumGapPx,
    )
    val rawSegments = splitTrackPathSegments(
        points = points,
        jumpThreshold = jumpThreshold,
        breakIndices = breakIndices,
    )
    val closureValid = closed && points.isSegmentedTrackPathClosedLoop(
        minimumClosurePx = minimumGapPx,
        gapMultiplier = gapMultiplier,
    )
    val mergedSegments = mergeClosedTrackPathSegments(
        rawSegments = rawSegments,
        jumpThreshold = jumpThreshold,
        closureValid = closureValid,
    )

    val sanitizedSegments = mergedSegments
        .map { segment -> segment.removeNearDuplicates() }
        .filter { segment -> segment.size >= 2 }

    return SegmentedTrackPath(
        segments = sanitizedSegments,
        closeLoop = closureValid && sanitizedSegments.size == 1,
    )
}

private fun splitTrackPathSegments(
    points: List<Offset>,
    jumpThreshold: Float,
    breakIndices: Set<Int>,
): List<MutableList<Offset>> {
    val rawSegments = mutableListOf<MutableList<Offset>>()
    var currentSegment = mutableListOf(points.first())

    for (index in 1 until points.size) {
        val previous = points[index - 1]
        val current = points[index]
        if (shouldStartNewTrackPathSegment(index, previous, current, jumpThreshold, breakIndices)) {
            rawSegments += currentSegment
            currentSegment = mutableListOf(current)
        } else {
            currentSegment += current
        }
    }
    rawSegments += currentSegment
    return rawSegments
}

private fun shouldStartNewTrackPathSegment(
    index: Int,
    previous: Offset,
    current: Offset,
    jumpThreshold: Float,
    breakIndices: Set<Int>,
): Boolean {
    val segmentLength = hypot(current.x - previous.x, current.y - previous.y)
    return index in breakIndices || !segmentLength.isFinite() || segmentLength > jumpThreshold
}

private fun mergeClosedTrackPathSegments(
    rawSegments: List<MutableList<Offset>>,
    jumpThreshold: Float,
    closureValid: Boolean,
): List<MutableList<Offset>> {
    if (!closureValid || rawSegments.size <= 1) return rawSegments
    val merged = rawSegments.toMutableList()
    val head = merged.firstOrNull()
    val tail = merged.lastOrNull()
    if (head == null || tail == null) return merged

    val closureLength = hypot(tail.last().x - head.first().x, tail.last().y - head.first().y)
    if (!closureLength.isFinite() || closureLength > jumpThreshold) return merged

    val mergedLoopSegment = buildList {
        addAll(tail)
        addAll(head)
    }
    merged.removeLast()
    merged.removeFirst()
    return mutableListOf(mergedLoopSegment.toMutableList()).apply { addAll(merged) }
}

private fun List<Offset>.removeNearDuplicates(
    minimumSegmentLengthPx: Float = segmentedTrackPathPointEpsilon,
): List<Offset> {
    if (size < 2) return this
    val sanitized = ArrayList<Offset>(size)
    forEach { point ->
        val previous = sanitized.lastOrNull()
        if (previous == null || hypot(point.x - previous.x, point.y - previous.y) > minimumSegmentLengthPx) {
            sanitized += point
        }
    }
    if (sanitized.size >= 2 && hypot(
            sanitized.last().x - sanitized.first().x,
            sanitized.last().y - sanitized.first().y,
        ) <= minimumSegmentLengthPx
    ) {
        sanitized.removeLast()
    }
    return sanitized
}

private fun List<Offset>.computeSegmentedTrackPathJumpThreshold(gapMultiplier: Float, minimumGapPx: Float): Float {
    val segmentLengths = buildList {
        for (index in 1 until this@computeSegmentedTrackPathJumpThreshold.size) {
            val previous = this@computeSegmentedTrackPathJumpThreshold[index - 1]
            val current = this@computeSegmentedTrackPathJumpThreshold[index]
            val length = hypot(current.x - previous.x, current.y - previous.y)
            if (length.isFinite() && length > segmentedTrackPathPointEpsilon) {
                add(length)
            }
        }
    }
    val median = segmentLengths.medianOrNull() ?: 0f
    return maxOf(minimumGapPx, median * gapMultiplier, segmentedTrackPathPointEpsilon)
}

private fun List<Offset>.isSegmentedTrackPathClosedLoop(
    minimumClosurePx: Float = 0f,
    gapMultiplier: Float = segmentedTrackPathGapMultiplier,
): Boolean {
    if (size < 3) return false
    val closureDistance = hypot(last().x - first().x, last().y - first().y)
    if (!closureDistance.isFinite()) return false
    val jumpThreshold = computeSegmentedTrackPathJumpThreshold(
        gapMultiplier = gapMultiplier,
        minimumGapPx = minimumClosurePx,
    )
    return closureDistance <= jumpThreshold
}
