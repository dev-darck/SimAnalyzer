package com.analyzer.trackmap.builder.recording.width

import com.analyzer.trackmap.builder.recording.geometry.findClosestIndex
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderRuntime
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderState
import com.analyzer.trackmap.data.library.TrackMapStatsCalculator
import com.project.analyzer.math.Vec2
import kotlin.math.abs

internal class TrackMapWidthProfiler(private val stats: TrackMapStatsCalculator, private val minPointsToSave: Int) {

    fun onLapAccepted(runtime: TrackMapRecorderRuntime, completedLapPoints: List<Vec2>): String? {
        if (completedLapPoints.size < minPointsToSave) return null

        if (runtime.centerlinePoints.isEmpty()) {
            runtime.centerlinePoints.addAll(completedLapPoints)
            runtime.ensureEdgeCapacity(runtime.centerlinePoints.size)
            runtime.trackDistanceMeters = stats.computeDistance(runtime.centerlinePoints)
            runtime.bounds = stats.computeBounds(runtime.centerlinePoints)
            return "Centerline captured. Save now, or drive track edges to refine widths."
        }

        runtime.ensureEdgeCapacity(runtime.centerlinePoints.size)
        absorbEdgeLap(runtime, completedLapPoints)
        runtime.bounds = stats.computeBounds(runtime.centerlinePoints)
        return null
    }

    fun resolvePointWidths(
        runtime: TrackMapRecorderRuntime,
        points: List<Vec2>,
        fallbackHalfWidthMeters: Float,
    ): WidthResolution {
        if (points.isEmpty()) return WidthResolution()

        val fallback = fallbackHalfWidthMeters
            .takeIf { it.isFinite() && it > 0f }
            ?: TrackMapRecorderState.DEFAULT_FALLBACK_HALF_WIDTH_METERS

        val left = if (runtime.centerlinePoints.size == points.size &&
            runtime.leftEdgeWidthMeters.size == points.size
        ) {
            resolveSideWidths(
                observed = runtime.leftEdgeWidthMeters,
                fallback = fallback,
            )
        } else {
            List(points.size) { fallback }
        }
        val right = if (runtime.centerlinePoints.size == points.size &&
            runtime.rightEdgeWidthMeters.size == points.size
        ) {
            resolveSideWidths(
                observed = runtime.rightEdgeWidthMeters,
                fallback = fallback,
            )
        } else {
            List(points.size) { fallback }
        }

        val averageWidth = (
            left.indices.sumOf { index ->
                (left[index] + right[index]).toDouble()
            } / left.size.coerceAtLeast(1)
            ).toFloat()

        return WidthResolution(
            leftWidthsMeters = left,
            rightWidthsMeters = right,
            averageTrackWidthMeters = averageWidth,
        )
    }

    fun buildGuidanceText(request: TrackMapGuidanceRequest): String? {
        if (!request.recording) return null
        if (request.points.size < 2) return "Lap 1: build centerline first."

        val target = resolveCaptureTarget(request.runtime)
        val baseHint = target.baseHint()
        val carPosition = request.currentPosition ?: return baseHint

        val nearestIndex = findNearestForwardIndex(
            points = request.points,
            target = carPosition,
            startIndex = request.runtime.lastGuidanceIndex,
        )
        request.runtime.lastGuidanceIndex = nearestIndex

        return buildEdgeGuidance(
            target = target,
            baseHint = baseHint,
            signedOffset = (carPosition - request.points[nearestIndex])
                .dot(localNormal(request.points, nearestIndex)),
            leftWidth = request.leftWidthsMeters.getOrElse(nearestIndex) { 0f },
            rightWidth = request.rightWidthsMeters.getOrElse(nearestIndex) { 0f },
        )
    }

    private fun absorbEdgeLap(runtime: TrackMapRecorderRuntime, lapPoints: List<Vec2>) {
        if (lapPoints.isEmpty() || runtime.centerlinePoints.size < 2) return
        runtime.ensureEdgeCapacity(runtime.centerlinePoints.size)

        var searchIndex = runtime.centerlinePoints.findClosestIndex(lapPoints.first())
        for (lapPoint in lapPoints) {
            searchIndex = findNearestForwardIndex(
                points = runtime.centerlinePoints,
                target = lapPoint,
                startIndex = searchIndex,
            )

            val center = runtime.centerlinePoints[searchIndex]
            val normal = localNormal(runtime.centerlinePoints, searchIndex)
            val signedOffset = (lapPoint - center).dot(normal)
            val absOffset = abs(signedOffset)
            if (!absOffset.isFinite() || absOffset > MAX_EDGE_CAPTURE_OFFSET_METERS) continue

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

    private fun resolveCaptureTarget(runtime: TrackMapRecorderRuntime): CaptureTarget = when {
        runtime.centerlinePoints.isEmpty() -> CaptureTarget.CENTERLINE
        runtime.leftCoverageRatio < TARGET_EDGE_COVERAGE_RATIO -> CaptureTarget.LEFT_EDGE
        runtime.rightCoverageRatio < TARGET_EDGE_COVERAGE_RATIO -> CaptureTarget.RIGHT_EDGE
        else -> CaptureTarget.COMPLETE
    }

    private fun localNormal(points: List<Vec2>, index: Int): Vec2 {
        val count = points.size
        if (count < 2) return Vec2.Up
        val prev = points[(index - 1 + count) % count]
        val next = points[(index + 1) % count]
        val tangent = (next - prev).safeNormalized(Vec2.Right)
        return tangent.perpLeft().safeNormalized(Vec2.Up)
    }

    private fun findNearestForwardIndex(points: List<Vec2>, target: Vec2, startIndex: Int): Int {
        if (points.isEmpty()) return 0
        val count = points.size
        val safeStart = startIndex.coerceIn(0, count - 1)
        var bestIdx = safeStart
        var bestDist = points[safeStart].distanceTo(target)
        val safeWindow = WIDTH_SEARCH_WINDOW.coerceAtMost(count - 1)
        for (offset in 1..safeWindow) {
            val forwardIdx = (safeStart + offset) % count
            val backwardIdx = (safeStart - offset + count) % count

            val forwardDist = points[forwardIdx].distanceTo(target)
            if (forwardDist < bestDist) {
                bestDist = forwardDist
                bestIdx = forwardIdx
            }

            val backwardDist = points[backwardIdx].distanceTo(target)
            if (backwardDist < bestDist) {
                bestDist = backwardDist
                bestIdx = backwardIdx
            }
        }
        return bestIdx
    }

    private fun resolveSideWidths(observed: List<Float>, fallback: Float): List<Float> {
        if (observed.isEmpty()) return emptyList()

        val known = observed.mapIndexedNotNull { index, raw ->
            if (!raw.isFinite() || raw < MIN_EDGE_OBSERVATION_METERS) return@mapIndexedNotNull null
            index to normalizeObservedWidth(raw)
        }
        if (known.isEmpty()) return List(observed.size) { fallback }

        if (known.size == 1) {
            val only = known.first()
            return List(observed.size) { index ->
                if (index == only.first) only.second else fallback
            }
        }

        val resolved = MutableList(observed.size) { fallback }
        for (idx in known.indices) {
            val (startIndex, startValue) = known[idx]
            val (endIndex, endValue) = known[(idx + 1) % known.size]
            val span = circularDistance(startIndex, endIndex, observed.size)
            if (span <= 0) {
                resolved[startIndex] = startValue
                continue
            }
            for (step in 0..span) {
                val pointIndex = (startIndex + step) % observed.size
                val t = step.toFloat() / span.toFloat()
                resolved[pointIndex] = lerp(startValue, endValue, t)
            }
        }

        return smoothCircular(resolved)
    }

    private fun normalizeObservedWidth(rawWidth: Float): Float =
        (rawWidth + EDGE_PADDING_METERS).coerceIn(MIN_SIDE_WIDTH_METERS, MAX_SIDE_WIDTH_METERS)

    private fun circularDistance(from: Int, to: Int, size: Int): Int {
        if (size <= 0) return 0
        return if (to >= from) {
            to - from
        } else {
            size - from + to
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun smoothCircular(values: List<Float>): List<Float> {
        if (values.size < 3) return values

        val size = values.size
        val smoothed = MutableList(size) { 0f }
        for (index in values.indices) {
            var weightedSum = 0f
            var weightTotal = 0f
            for (offset in -WIDTH_SMOOTHING_RADIUS..WIDTH_SMOOTHING_RADIUS) {
                val sampleIndex = (index + offset + size) % size
                val weight = (WIDTH_SMOOTHING_RADIUS + 1 - abs(offset)).toFloat()
                weightedSum += values[sampleIndex] * weight
                weightTotal += weight
            }
            smoothed[index] = if (weightTotal > 0f) weightedSum / weightTotal else values[index]
        }
        return smoothed
    }

    private fun buildEdgeGuidance(
        target: CaptureTarget,
        baseHint: String,
        signedOffset: Float,
        leftWidth: Float,
        rightWidth: Float,
    ): String {
        val targetOffset = when (target) {
            CaptureTarget.CENTERLINE,
            CaptureTarget.COMPLETE,
            -> return baseHint

            CaptureTarget.LEFT_EDGE -> leftWidth * GUIDANCE_EDGE_TARGET_RATIO

            CaptureTarget.RIGHT_EDGE -> -rightWidth * GUIDANCE_EDGE_TARGET_RATIO
        }
        val error = targetOffset - signedOffset
        val absError = abs(error)
        if (!absError.isFinite()) return baseHint
        if (absError <= GUIDANCE_TOLERANCE_METERS) return "$baseHint · hold line"

        val direction = if (error > 0f) "move LEFT" else "move RIGHT"
        return "$baseHint · $direction ${"%.1f".format(absError)}m"
    }

    private fun CaptureTarget.baseHint(): String = when (this) {
        CaptureTarget.CENTERLINE -> "Lap 1: build centerline"
        CaptureTarget.LEFT_EDGE -> "Optional: drive close to LEFT edge to improve widths"
        CaptureTarget.RIGHT_EDGE -> "Optional: drive close to RIGHT edge to improve widths"
        CaptureTarget.COMPLETE -> "Width coverage is good"
    }

    data class WidthResolution(
        val leftWidthsMeters: List<Float> = emptyList(),
        val rightWidthsMeters: List<Float> = emptyList(),
        val averageTrackWidthMeters: Float = TrackMapRecorderState.DEFAULT_AVERAGE_TRACK_WIDTH_METERS,
    )

    data class TrackMapGuidanceRequest(
        val runtime: TrackMapRecorderRuntime,
        val recording: Boolean,
        val currentPosition: Vec2?,
        val points: List<Vec2>,
        val leftWidthsMeters: List<Float>,
        val rightWidthsMeters: List<Float>,
    )

    private enum class CaptureTarget {
        CENTERLINE,
        LEFT_EDGE,
        RIGHT_EDGE,
        COMPLETE,
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
        const val MAX_EDGE_CAPTURE_OFFSET_METERS = 25f
        const val WIDTH_SMOOTHING_RADIUS = 2
    }
}
