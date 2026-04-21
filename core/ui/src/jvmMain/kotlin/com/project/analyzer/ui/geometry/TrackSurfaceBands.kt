package com.project.analyzer.ui.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.hypot

private const val trackSurfaceBandSegmentEpsilon = 1e-4f
private const val trackSurfaceBandDefaultLoopGapMultiplier = 6f

public fun buildTrackSurfaceBandPaths(
    centerLine: List<Offset>,
    leftEdge: List<Offset>,
    rightEdge: List<Offset>,
    gapMultiplier: Float,
    minimumGapPx: Float,
): List<Path> {
    val pointCount = minOf(centerLine.size, leftEdge.size, rightEdge.size)
    if (pointCount < 2) return emptyList()

    val safeCenterLine = centerLine.take(pointCount)
    val safeLeftEdge = leftEdge.take(pointCount)
    val safeRightEdge = rightEdge.take(pointCount)
    val jumpThresholds = TrackSurfaceBandJumpThresholds(
        center = safeCenterLine.computeTrackPathJumpThreshold(
            gapMultiplier = gapMultiplier,
            minimumGapPx = minimumGapPx,
        ),
        left = safeLeftEdge.computeTrackPathJumpThreshold(
            gapMultiplier = gapMultiplier,
            minimumGapPx = minimumGapPx,
        ),
        right = safeRightEdge.computeTrackPathJumpThreshold(
            gapMultiplier = gapMultiplier,
            minimumGapPx = minimumGapPx,
        ),
    )
    val bandPaths = ArrayList<Path>(pointCount)

    fun addBand(startIndex: Int, endIndex: Int) {
        val startCenter = safeCenterLine[startIndex]
        val endCenter = safeCenterLine[endIndex]
        val startLeft = safeLeftEdge[startIndex]
        val endLeft = safeLeftEdge[endIndex]
        val startRight = safeRightEdge[startIndex]
        val endRight = safeRightEdge[endIndex]
        val segment = TrackSurfaceBandSegment(
            startCenter = startCenter,
            endCenter = endCenter,
            startLeft = startLeft,
            endLeft = endLeft,
            startRight = startRight,
            endRight = endRight,
        )
        if (!isTrackSurfaceBandUsable(segment = segment, jumpThresholds = jumpThresholds)) {
            return
        }

        bandPaths += Path().apply {
            moveTo(segment.startLeft.x, segment.startLeft.y)
            lineTo(segment.endLeft.x, segment.endLeft.y)
            lineTo(segment.endRight.x, segment.endRight.y)
            lineTo(segment.startRight.x, segment.startRight.y)
            close()
        }
    }

    for (index in 1 until pointCount) {
        addBand(startIndex = index - 1, endIndex = index)
    }

    if (
        safeCenterLine.isTrackPathClosedLoop(
            minimumClosurePx = minimumGapPx,
            gapMultiplier = gapMultiplier,
        )
    ) {
        addBand(startIndex = pointCount - 1, endIndex = 0)
    }

    return bandPaths
}

public fun buildSegmentedTrackPath(
    points: List<Offset>,
    gapMultiplier: Float,
    minimumGapPx: Float,
    closeLoop: Boolean = false,
): Path {
    if (points.isEmpty()) return Path()
    val path = Path()
    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) return path

    val jumpThreshold = points.computeTrackPathJumpThreshold(
        gapMultiplier = gapMultiplier,
        minimumGapPx = minimumGapPx,
    )
    for (index in 1 until points.size) {
        val previous = points[index - 1]
        val current = points[index]
        val distance = hypot(current.x - previous.x, current.y - previous.y)
        if (distance > jumpThreshold) {
            path.moveTo(current.x, current.y)
        } else {
            path.lineTo(current.x, current.y)
        }
    }
    if (closeLoop && points.isTrackPathClosedLoop(minimumClosurePx = minimumGapPx, gapMultiplier = gapMultiplier)) {
        path.lineTo(points.first().x, points.first().y)
    }
    return path
}

public fun List<Offset>.isTrackPathClosedLoop(
    minimumClosurePx: Float = 0f,
    gapMultiplier: Float = trackSurfaceBandDefaultLoopGapMultiplier,
): Boolean {
    if (size < 3) return false
    val closureDistance = hypot(last().x - first().x, last().y - first().y)
    if (!closureDistance.isFinite()) return false
    val jumpThreshold = computeTrackPathJumpThreshold(
        gapMultiplier = gapMultiplier,
        minimumGapPx = minimumClosurePx,
    )
    return closureDistance <= jumpThreshold
}

public fun List<Offset>.computeTrackPathJumpThreshold(gapMultiplier: Float, minimumGapPx: Float): Float {
    val segmentLengths = buildList {
        for (index in 1 until this@computeTrackPathJumpThreshold.size) {
            val previous = this@computeTrackPathJumpThreshold[index - 1]
            val current = this@computeTrackPathJumpThreshold[index]
            val length = hypot(current.x - previous.x, current.y - previous.y)
            if (length.isFinite() && length > 0.5f) add(length)
        }
    }
    val median = segmentLengths.medianOrNull() ?: 0f
    return maxOf(minimumGapPx, median * gapMultiplier)
}

private fun isTrackSurfaceBandUsable(
    segment: TrackSurfaceBandSegment,
    jumpThresholds: TrackSurfaceBandJumpThresholds,
): Boolean {
    val centerLength = segment.startCenter.distanceTo(segment.endCenter)
    val leftLength = segment.startLeft.distanceTo(segment.endLeft)
    val rightLength = segment.startRight.distanceTo(segment.endRight)
    val crossSectionLengths = listOf(
        segment.startLeft.distanceTo(segment.startRight),
        segment.endLeft.distanceTo(segment.endRight),
    )
    val longitudinalLengthsAreValid = centerLength.isFinite() &&
        centerLength > trackSurfaceBandSegmentEpsilon &&
        centerLength <= jumpThresholds.center &&
        leftLength.isFinite() &&
        leftLength > trackSurfaceBandSegmentEpsilon &&
        leftLength <= jumpThresholds.left &&
        rightLength.isFinite() &&
        rightLength > trackSurfaceBandSegmentEpsilon &&
        rightLength <= jumpThresholds.right
    return longitudinalLengthsAreValid && crossSectionLengths.all { length ->
        length.isFinite() && length > trackSurfaceBandSegmentEpsilon
    }
}

private data class TrackSurfaceBandSegment(
    val startCenter: Offset,
    val endCenter: Offset,
    val startLeft: Offset,
    val endLeft: Offset,
    val startRight: Offset,
    val endRight: Offset,
)

private data class TrackSurfaceBandJumpThresholds(val center: Float, val left: Float, val right: Float)

private fun Offset.distanceTo(other: Offset): Float = hypot(other.x - x, other.y - y)

private fun List<Float>.medianOrNull(): Float? {
    if (isEmpty()) return null
    val sorted = sorted()
    val middleIndex = sorted.lastIndex / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middleIndex] + sorted[middleIndex + 1]) * 0.5f
    } else {
        sorted[middleIndex]
    }
}
