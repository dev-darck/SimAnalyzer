package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import dev.zacsweers.metro.Inject
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.sqrt

@Inject
internal class AcEvoTrackCenterLineResolver {

    fun resolve(
        splineSamples: List<AcEvoTrackSample>,
        controlPoints: List<AcEvoTrackSample>,
        idealLine: List<AcEvoTrackSample>,
    ): List<AcEvoTrackSample> {
        if (splineSamples.size >= 2) return splineSamples

        val interpolatedControlPoints = controlPoints
            .takeIf { points -> points.size >= 2 }
            ?.let(::interpolateControlPoints)
        if (interpolatedControlPoints != null) {
            if (idealLine.size >= 2 && !interpolatedControlPoints.isPlausibleAgainst(idealLine)) {
                return idealLine
            }
            return interpolatedControlPoints
        }

        return idealLine.takeIf { points -> points.size >= 2 } ?: emptyList()
    }

    private fun interpolateControlPoints(points: List<AcEvoTrackSample>): List<AcEvoTrackSample> {
        if (points.size < 2) return points

        val result = ArrayList<AcEvoTrackSample>(points.size * 4)
        for (index in 0 until points.lastIndex) {
            val start = points[index]
            val end = points[index + 1]
            val distance = hypot(end.x - start.x, end.y - start.y)
            val segments = maxOf(1, ceil(distance / CONTROL_POINT_STEP_METERS).toInt())
            repeat(segments) { segmentIndex ->
                val t = segmentIndex.toFloat() / segments.toFloat()
                result += hermiteSample(
                    start = start,
                    end = end,
                    startTangent = tangentAt(points, index, distance),
                    endTangent = tangentAt(points, index + 1, distance),
                    t = t,
                )
            }
        }
        result += points.last()
        return removeNearDuplicates(result)
    }

    private fun tangentAt(
        points: List<AcEvoTrackSample>,
        index: Int,
        scale: Float,
    ): Pair<Float, Float> {
        val point = points[index]
        val explicitTangent = normalize(point.forwardX, point.forwardY)
        if (explicitTangent != null) {
            return explicitTangent.first * scale to explicitTangent.second * scale
        }

        val previous = points.getOrNull(index - 1)
        val next = points.getOrNull(index + 1)
        val derived = when {
            previous != null && next != null -> normalize(next.x - previous.x, next.y - previous.y)
            next != null -> normalize(next.x - point.x, next.y - point.y)
            previous != null -> normalize(point.x - previous.x, point.y - previous.y)
            else -> null
        } ?: (0f to 0f)
        return derived.first * scale to derived.second * scale
    }

    private fun hermiteSample(
        start: AcEvoTrackSample,
        end: AcEvoTrackSample,
        startTangent: Pair<Float, Float>,
        endTangent: Pair<Float, Float>,
        t: Float,
    ): AcEvoTrackSample {
        val tt = t * t
        val ttt = tt * t
        val h00 = 2f * ttt - 3f * tt + 1f
        val h10 = ttt - 2f * tt + t
        val h01 = -2f * ttt + 3f * tt
        val h11 = ttt - tt

        return AcEvoTrackSample(
            x = h00 * start.x + h10 * startTangent.first + h01 * end.x + h11 * endTangent.first,
            y = h00 * start.y + h10 * startTangent.second + h01 * end.y + h11 * endTangent.second,
            leftWidthMeters = lerp(start.leftWidthMeters, end.leftWidthMeters, t),
            rightWidthMeters = lerp(start.rightWidthMeters, end.rightWidthMeters, t),
        )
    }

    private fun normalize(x: Float?, y: Float?): Pair<Float, Float>? {
        val xValue = x?.takeIf(Float::isFinite) ?: return null
        val yValue = y?.takeIf(Float::isFinite) ?: return null
        val length = sqrt(xValue * xValue + yValue * yValue)
        if (!length.isFinite() || length <= 1e-3f) return null
        return xValue / length to yValue / length
    }

    private fun removeNearDuplicates(points: List<AcEvoTrackSample>): List<AcEvoTrackSample> {
        if (points.size < 2) return points
        val result = ArrayList<AcEvoTrackSample>(points.size)
        points.forEach { point ->
            val previous = result.lastOrNull()
            if (previous == null || hypot(point.x - previous.x, point.y - previous.y) >= DUPLICATE_STEP_METERS) {
                result += point
            }
        }
        return result
    }

    private fun lerp(start: Float, end: Float, t: Float): Float = start + (end - start) * t

    private fun List<AcEvoTrackSample>.isPlausibleAgainst(reference: List<AcEvoTrackSample>): Boolean {
        val candidateLengthMeters = polylineLengthMeters()
        val referenceLengthMeters = reference.polylineLengthMeters()
        if (!candidateLengthMeters.isFinite() || !referenceLengthMeters.isFinite() || referenceLengthMeters <= 1f) {
            return false
        }

        val lengthRatio = candidateLengthMeters / referenceLengthMeters
        return lengthRatio in MinimumPlausibleLengthRatio..MaximumPlausibleLengthRatio
    }

    private fun List<AcEvoTrackSample>.polylineLengthMeters(): Float {
        if (size < 2) return 0f
        var lengthMeters = 0f
        for (index in 1..lastIndex) {
            val previous = this[index - 1]
            val current = this[index]
            lengthMeters += hypot(current.x - previous.x, current.y - previous.y)
        }
        return lengthMeters
    }

    private companion object {

        const val CONTROL_POINT_STEP_METERS = 4f
        const val DUPLICATE_STEP_METERS = 0.15f
        const val MinimumPlausibleLengthRatio = 0.65f
        const val MaximumPlausibleLengthRatio = 1.55f
    }
}
