package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.report.common.IssueSeverity
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Shared helper math and lookups keep the split analytics files lightweight and consistent.
 */
internal fun approximateTrackLengthMeters(trackMap: SessionAnalysisTrackMap?): Float {
    val points = trackMap?.points.orEmpty()
    if (points.size < 2) return ComprehensiveSessionAnalysisResolverConstants.DefaultTrackLengthMeters
    val distance = points.zipWithNext().sumOf { (current, next) ->
        hypot(
            next.x - current.x,
            next.y - current.y,
        ).toDouble()
    }
    return distance.toFloat().takeIf { it > 0f }
        ?: ComprehensiveSessionAnalysisResolverConstants.DefaultTrackLengthMeters
}

internal fun List<SessionAnalysisSample>.filterByTrackWindow(start: Float, end: Float): List<SessionAnalysisSample> =
    filter { sample ->
        val position = sample.trackPosition ?: return@filter false
        if (start <= end) position in start..end else position >= start || position <= end
    }

internal fun List<SessionAnalysisSample>.windowDurationMs(): Float = if (size < 2) {
    0f
} else {
    ((last().timestampNs - first().timestampNs) / 1_000_000f).coerceAtLeast(
        0f,
    )
}

internal fun List<Float>.smoothness(scale: Float): Float {
    if (size < 2) return 1f
    val avgDelta = zipWithNext { previous, next -> abs(next - previous) }.averageOrNull() ?: return 1f
    return (1f - avgDelta / scale).coerceIn(0f, 1f)
}

internal fun List<SessionAnalysisSample>.durationAboveThreshold(selector: (SessionAnalysisSample) -> Float?): Long {
    val selected = filter { (selector(it) ?: 0f) >= 0.15f }
    return selected.windowDurationMs().roundToLong().coerceAtLeast(if (selected.isNotEmpty()) 16L else 0L)
}

internal fun Long.toIssueSeverity(): IssueSeverity = when {
    this >= ComprehensiveSessionAnalysisResolverConstants.CriticalDeltaMs -> IssueSeverity.CRITICAL
    this >= ComprehensiveSessionAnalysisResolverConstants.WarningDeltaMs -> IssueSeverity.WARNING
    this <= -ComprehensiveSessionAnalysisResolverConstants.WarningDeltaMs -> IssueSeverity.POSITIVE
    else -> IssueSeverity.NEUTRAL
}

internal fun <T> List<T>.dominantOr(fallback: T): T = groupingBy {
    it
}.eachCount().maxByOrNull { (_, count) -> count }?.key ?: fallback

internal fun List<Float>.spread(): Float = (maxOrNull() ?: 0f) - (minOrNull() ?: 0f)

internal fun List<Float>.averageAbsOrNull(): Float? = map(::abs).averageOrNull()

internal fun Iterable<Number>.averageOrNull(): Float? {
    val values = toList()
    return if (values.isEmpty()) null else values.map(Number::toDouble).average().toFloat()
}

internal fun List<Number>.standardDeviation(): Float? {
    if (size < 2) return null
    val mean = map(Number::toDouble).average()
    val variance = sumOf { value ->
        val delta = value.toDouble() - mean
        delta * delta
    } / size.toDouble()
    return sqrt(variance).toFloat()
}

internal fun <T> List<T>.firstNotNullOfOrNull(selector: (T) -> Float?): Float? {
    for (item in this) selector(item)?.let { return it }
    return null
}

internal fun <T> List<T>.lastNotNullOfOrNull(selector: (T) -> Float?): Float? {
    for (index in indices.reversed()) selector(this[index])?.let { return it }
    return null
}
