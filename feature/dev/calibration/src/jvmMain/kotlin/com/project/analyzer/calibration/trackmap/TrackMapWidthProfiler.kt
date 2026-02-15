package com.project.analyzer.calibration.trackmap

import com.project.analyzer.math.Vec2
import kotlin.math.abs

class TrackMapWidthProfiler(
    private val stats: TrackMapStatsCalculator,
    private val minPointsToSave: Int
) {

    fun onLapAccepted(
        runtime: TrackMapRecorderRuntime,
        completedLapPoints: List<Vec2>
    ): String? {
        if (completedLapPoints.size < minPointsToSave) return null

        if (runtime.centerlinePoints.isEmpty()) {
            runtime.centerlinePoints.addAll(completedLapPoints)
            runtime.ensureEdgeCapacity(runtime.centerlinePoints.size)
            runtime.trackDistanceMeters = stats.computeDistance(runtime.centerlinePoints)
            runtime.bounds = stats.computeBounds(runtime.centerlinePoints)
            return "Centerline captured. Next lap: drive LEFT edge."
        }

        runtime.ensureEdgeCapacity(runtime.centerlinePoints.size)
        absorbEdgeLap(runtime, completedLapPoints)
        runtime.bounds = stats.computeBounds(runtime.centerlinePoints)
        return null
    }

    fun resolvePointWidths(
        runtime: TrackMapRecorderRuntime,
        points: List<Vec2>,
        fallbackHalfWidthMeters: Float
    ): WidthResolution {
        if (points.isEmpty()) return WidthResolution()

        val fallback = fallbackHalfWidthMeters
            .takeIf { it.isFinite() && it > 0f }
            ?: TrackMapRecorderState.DEFAULT_FALLBACK_HALF_WIDTH_METERS

        val left = MutableList(points.size) { fallback }
        val right = MutableList(points.size) { fallback }

        if (runtime.centerlinePoints.size == points.size &&
            runtime.leftEdgeWidthMeters.size == points.size &&
            runtime.rightEdgeWidthMeters.size == points.size
        ) {
            for (index in points.indices) {
                val observedLeft = runtime.leftEdgeWidthMeters[index]
                val observedRight = runtime.rightEdgeWidthMeters[index]

                left[index] = when {
                    observedLeft >= MIN_EDGE_OBSERVATION_METERS ->
                        (observedLeft + EDGE_PADDING_METERS).coerceIn(MIN_SIDE_WIDTH_METERS, MAX_SIDE_WIDTH_METERS)

                    else -> fallback
                }
                right[index] = when {
                    observedRight >= MIN_EDGE_OBSERVATION_METERS ->
                        (observedRight + EDGE_PADDING_METERS).coerceIn(MIN_SIDE_WIDTH_METERS, MAX_SIDE_WIDTH_METERS)

                    else -> fallback
                }
            }
        }

        val averageWidth = (left.indices.sumOf { index ->
            (left[index] + right[index]).toDouble()
        } / left.size.coerceAtLeast(1)).toFloat()

        return WidthResolution(
            leftWidthsMeters = left,
            rightWidthsMeters = right,
            averageTrackWidthMeters = averageWidth
        )
    }

    fun buildGuidanceText(
        runtime: TrackMapRecorderRuntime,
        recording: Boolean,
        currentPosition: Vec2?,
        points: List<Vec2>,
        leftWidthsMeters: List<Float>,
        rightWidthsMeters: List<Float>
    ): String? {
        if (!recording) return null
        if (points.size < 2) return "Lap 1: build centerline first."

        val target = resolveCaptureTarget(runtime)
        val baseHint = when (target) {
            CaptureTarget.CENTERLINE -> "Lap 1: build centerline"
            CaptureTarget.LEFT_EDGE -> "Drive close to LEFT edge"
            CaptureTarget.RIGHT_EDGE -> "Drive close to RIGHT edge"
            CaptureTarget.COMPLETE -> "Width coverage is good"
        }

        val carPos = currentPosition ?: return baseHint
        val nearest = findNearestForwardIndex(
            points = points,
            target = carPos,
            startIndex = runtime.lastGuidanceIndex,
            window = WIDTH_SEARCH_WINDOW
        )
        runtime.lastGuidanceIndex = nearest
        val center = points[nearest]
        val normal = localNormal(points, nearest)
        val signedOffset = (carPos - center).dot(normal)

        val targetOffset = when (target) {
            CaptureTarget.CENTERLINE -> 0f
            CaptureTarget.LEFT_EDGE -> leftWidthsMeters.getOrElse(nearest) { 0f } * GUIDANCE_EDGE_TARGET_RATIO
            CaptureTarget.RIGHT_EDGE -> -rightWidthsMeters.getOrElse(nearest) { 0f } * GUIDANCE_EDGE_TARGET_RATIO
            CaptureTarget.COMPLETE -> 0f
        }

        val error = targetOffset - signedOffset
        val absError = abs(error)
        if (target == CaptureTarget.COMPLETE || target == CaptureTarget.CENTERLINE) return baseHint
        if (!absError.isFinite()) return baseHint
        if (absError <= GUIDANCE_TOLERANCE_METERS) return "$baseHint · hold line"
        val direction = if (error > 0f) "move LEFT" else "move RIGHT"
        return "$baseHint · $direction ${"%.1f".format(absError)}m"
    }

    private fun absorbEdgeLap(
        runtime: TrackMapRecorderRuntime,
        lapPoints: List<Vec2>
    ) {
        if (lapPoints.isEmpty() || runtime.centerlinePoints.size < 2) return
        runtime.ensureEdgeCapacity(runtime.centerlinePoints.size)

        var searchIndex = findClosestIndex(runtime.centerlinePoints, lapPoints.first())
        for (lapPoint in lapPoints) {
            searchIndex = findNearestForwardIndex(
                points = runtime.centerlinePoints,
                target = lapPoint,
                startIndex = searchIndex,
                window = WIDTH_SEARCH_WINDOW
            )

            val center = runtime.centerlinePoints[searchIndex]
            val normal = localNormal(runtime.centerlinePoints, searchIndex)
            val signedOffset = (lapPoint - center).dot(normal)

            if (signedOffset >= MIN_EDGE_OBSERVATION_METERS) {
                runtime.leftEdgeWidthMeters[searchIndex] =
                    maxOf(runtime.leftEdgeWidthMeters[searchIndex], signedOffset)
            } else if (signedOffset <= -MIN_EDGE_OBSERVATION_METERS) {
                runtime.rightEdgeWidthMeters[searchIndex] =
                    maxOf(runtime.rightEdgeWidthMeters[searchIndex], -signedOffset)
            }
        }

        val count = runtime.centerlinePoints.size.coerceAtLeast(1)
        val leftCovered = runtime.leftEdgeWidthMeters.count { it >= MIN_EDGE_COVERAGE_METERS }
        val rightCovered = runtime.rightEdgeWidthMeters.count { it >= MIN_EDGE_COVERAGE_METERS }
        runtime.leftCoverageRatio = leftCovered.toFloat() / count
        runtime.rightCoverageRatio = rightCovered.toFloat() / count
    }

    private fun resolveCaptureTarget(runtime: TrackMapRecorderRuntime): CaptureTarget {
        return when {
            runtime.centerlinePoints.isEmpty() -> CaptureTarget.CENTERLINE
            runtime.leftCoverageRatio < TARGET_EDGE_COVERAGE_RATIO -> CaptureTarget.LEFT_EDGE
            runtime.rightCoverageRatio < TARGET_EDGE_COVERAGE_RATIO -> CaptureTarget.RIGHT_EDGE
            else -> CaptureTarget.COMPLETE
        }
    }

    private fun localNormal(points: List<Vec2>, index: Int): Vec2 {
        val count = points.size
        if (count < 2) return Vec2.Up
        val prev = points[(index - 1 + count) % count]
        val next = points[(index + 1) % count]
        val tangent = (next - prev).safeNormalized(Vec2.Right)
        return tangent.perpLeft().safeNormalized(Vec2.Up)
    }

    private fun findNearestForwardIndex(
        points: List<Vec2>,
        target: Vec2,
        startIndex: Int,
        window: Int
    ): Int {
        if (points.isEmpty()) return 0
        val count = points.size
        val safeStart = startIndex.coerceIn(0, count - 1)
        var bestIdx = safeStart
        var bestDist = points[safeStart].distanceTo(target)
        val safeWindow = window.coerceAtMost(count)
        for (offset in 1..safeWindow) {
            val idx = (safeStart + offset) % count
            val dist = points[idx].distanceTo(target)
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = idx
            }
        }
        return bestIdx
    }

    private fun findClosestIndex(points: List<Vec2>, target: Vec2): Int {
        if (points.isEmpty()) return -1
        var bestIdx = 0
        var bestDistSq = (points[0] - target).len2()
        for (i in 1 until points.size) {
            val distSq = (points[i] - target).len2()
            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestIdx = i
            }
        }
        return bestIdx
    }

    data class WidthResolution(
        val leftWidthsMeters: List<Float> = emptyList(),
        val rightWidthsMeters: List<Float> = emptyList(),
        val averageTrackWidthMeters: Float = TrackMapRecorderState.DEFAULT_AVERAGE_TRACK_WIDTH_METERS
    )

    private enum class CaptureTarget {
        CENTERLINE,
        LEFT_EDGE,
        RIGHT_EDGE,
        COMPLETE
    }

    companion object {

        const val WIDTH_SEARCH_WINDOW = 160
        const val MIN_EDGE_OBSERVATION_METERS = 0.35f
        const val MIN_EDGE_COVERAGE_METERS = 1.0f
        const val TARGET_EDGE_COVERAGE_RATIO = 0.55f
        const val GUIDANCE_EDGE_TARGET_RATIO = 0.85f
        const val GUIDANCE_TOLERANCE_METERS = 0.45f
        const val EDGE_PADDING_METERS = 0.35f
        const val MIN_SIDE_WIDTH_METERS = 2.5f
        const val MAX_SIDE_WIDTH_METERS = 15f
    }
}
