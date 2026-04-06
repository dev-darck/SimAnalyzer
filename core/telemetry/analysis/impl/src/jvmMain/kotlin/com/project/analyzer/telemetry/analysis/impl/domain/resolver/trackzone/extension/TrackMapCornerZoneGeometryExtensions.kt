package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.degreesPerRadian
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorMaximumResampleStepMeters
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorMinimumResampleCount
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorMinimumResampleStepMeters
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorTargetResamplePoints
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.model.ClosedTrackPath
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.model.ResampledTrackPoint
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Geometry helpers support radius, direction, and spacing checks during corner-zone detection.
 */
internal fun List<SessionAnalysisTrackMapPoint>.sanitizeTrackPoints(): List<SessionAnalysisTrackMapPoint> {
    if (size <= 1) return this
    val sanitized = ArrayList<SessionAnalysisTrackMapPoint>(size)
    forEach { point ->
        val previous = sanitized.lastOrNull()
        if (previous == null || distanceBetween(previous, point) >= 0.35f) {
            sanitized += point
        }
    }
    if (sanitized.size <= 2) return sanitized
    val first = sanitized.first()
    val last = sanitized.last()
    return if (distanceBetween(first, last) <= 0.35f) {
        sanitized.dropLast(1)
    } else {
        sanitized
    }
}

internal fun distanceBetween(first: SessionAnalysisTrackMapPoint, second: SessionAnalysisTrackMapPoint): Float =
    hypot(second.x - first.x, second.y - first.y)

internal fun List<SessionAnalysisTrackMapPoint>.toClosedPath(): ClosedTrackPath? {
    if (size < 3) return null
    val closureGap = distanceBetween(first(), last())
    val segmentLengths = MutableList(size) { 0f }
    var totalDistance = 0f
    for (index in indices) {
        val next = this[wrapIndex(index + 1)]
        val length = distanceBetween(this[index], next)
        if (!length.isFinite() || length <= 0.001f) continue
        segmentLengths[index] = length
        totalDistance += length
    }
    if (!totalDistance.isFinite() || totalDistance <= 0.001f) return null
    val maximumClosureGap = max(40f, totalDistance * 0.12f)
    if (closureGap > maximumClosureGap) return null
    return ClosedTrackPath(
        points = this,
        segmentLengths = segmentLengths,
        totalDistance = totalDistance,
    )
}

internal fun ClosedTrackPath.resample(): List<ResampledTrackPoint> {
    val idealStep = (totalDistance / detectorTargetResamplePoints.toFloat())
        .coerceIn(detectorMinimumResampleStepMeters, detectorMaximumResampleStepMeters)
    val sampleCount = max(
        detectorMinimumResampleCount,
        (totalDistance / idealStep).roundToInt(),
    )
    if (sampleCount <= 1) return emptyList()

    val step = totalDistance / sampleCount.toFloat()
    val result = ArrayList<ResampledTrackPoint>(sampleCount)
    var segmentIndex = 0
    var segmentStartDistance = 0f
    var segmentLength = segmentLengths[segmentIndex]

    repeat(sampleCount) { sampleIndex ->
        val targetDistance = sampleIndex * step
        while (
            segmentIndex < segmentLengths.lastIndex &&
            segmentStartDistance + segmentLength < targetDistance
        ) {
            segmentStartDistance += segmentLength
            segmentIndex += 1
            segmentLength = segmentLengths[segmentIndex]
        }
        val start = points[segmentIndex]
        val end = points[points.wrapIndex(segmentIndex + 1)]
        val fraction = if (segmentLength <= 0.0001f) {
            0f
        } else {
            ((targetDistance - segmentStartDistance) / segmentLength).coerceIn(0f, 1f)
        }
        result += ResampledTrackPoint(
            x = lerp(start.x, end.x, fraction),
            y = lerp(start.y, end.y, fraction),
            distanceMeters = targetDistance,
        )
    }
    return result
}

internal fun List<ResampledTrackPoint>.radiusForMeters(windowMeters: Float): Int {
    if (size <= 2) return 1
    val totalDistance = (last().distanceMeters - first().distanceMeters) +
        hypot(
            first().x - last().x,
            first().y - last().y,
        )
    val stepMeters = (totalDistance / size.toFloat())
        .takeIf { value -> value.isFinite() && value > 0.05f }
        ?: 1f
    return max(1, (windowMeters / stepMeters).roundToInt())
}

internal fun List<ResampledTrackPoint>.gaussianSmooth(radius: Int): List<ResampledTrackPoint> {
    if (size < 3 || radius <= 0) return this
    val sigma = (radius / 2f).coerceAtLeast(1f)
    val weights = (-radius..radius).map { offset ->
        exp(-(offset * offset) / (2f * sigma * sigma))
    }

    return indices.map { index ->
        var weightedX = 0f
        var weightedY = 0f
        var totalWeight = 0f

        for (offset in -radius..radius) {
            val sourceIndex = wrapIndex(index + offset)
            val weight = weights[offset + radius]
            weightedX += this[sourceIndex].x * weight
            weightedY += this[sourceIndex].y * weight
            totalWeight += weight
        }

        ResampledTrackPoint(
            x = weightedX / totalWeight,
            y = weightedY / totalWeight,
            distanceMeters = this[index].distanceMeters,
        )
    }
}

internal fun List<ResampledTrackPoint>.computeHeadings(window: Int): List<Float> = indices.map { index ->
    val previous = this[wrapIndex(index - window)]
    val next = this[wrapIndex(index + window)]
    atan2(next.y - previous.y, next.x - previous.x)
}

internal fun List<Float>.computeLocalTurnRadians(): List<Float> = indices.map { index ->
    normalizeHeadingDeltaRadians(
        value = this[index] - this[wrapIndex(index - 1)],
    )
}

internal fun List<Float>.computeSupportTurnDegrees(window: Int): List<Float> = indices.map { index ->
    normalizeHeadingDeltaRadians(
        value = this[wrapIndex(index + window)] - this[wrapIndex(index - window)],
    ) * degreesPerRadian
}
