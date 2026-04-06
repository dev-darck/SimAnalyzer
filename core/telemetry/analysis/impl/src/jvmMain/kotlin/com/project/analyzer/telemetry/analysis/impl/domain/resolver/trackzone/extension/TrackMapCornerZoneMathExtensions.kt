package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension

import kotlin.math.PI
import kotlin.math.abs

/**
 * Keeps low-level numeric operations close to zone detection without leaking math noise into the detector.
 */
internal fun normalizeHeadingDeltaRadians(value: Float): Float {
    val fullTurn = (PI * 2f).toFloat()
    val halfTurn = PI.toFloat()
    var normalized = value
    while (normalized > halfTurn) normalized -= fullTurn
    while (normalized < -halfTurn) normalized += fullTurn
    return normalized
}

internal fun wrapAwareMidpoint(startTrackPosition: Float, endTrackPosition: Float): Float {
    val normalizedStart = startTrackPosition.normalizeTrackPosition()
    val normalizedEnd = endTrackPosition.normalizeTrackPosition()
    val wrappedEnd = if (normalizedEnd < normalizedStart) normalizedEnd + 1f else normalizedEnd
    return ((normalizedStart + wrappedEnd) / 2f).normalizeTrackPosition()
}

internal fun Float.normalizeTrackPosition(): Float {
    if (!isFinite()) return 0f
    var normalized = this % 1f
    if (normalized < 0f) normalized += 1f
    return normalized
}

internal fun trackPositionDistance(firstTrackPosition: Float, secondTrackPosition: Float): Float {
    val distance = abs(
        firstTrackPosition.normalizeTrackPosition() - secondTrackPosition.normalizeTrackPosition(),
    )
    return minOf(distance, 1f - distance)
}

internal fun wrapIndex(index: Int, size: Int): Int {
    if (size <= 0) return 0
    var normalized = index % size
    if (normalized < 0) normalized += size
    return normalized
}

internal fun <T> List<T>.wrapIndex(index: Int): Int = wrapIndex(index = index, size = size)

internal fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction
