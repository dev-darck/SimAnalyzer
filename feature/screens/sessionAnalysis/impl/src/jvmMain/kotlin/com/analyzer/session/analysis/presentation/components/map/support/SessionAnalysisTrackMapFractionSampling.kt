package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlin.math.abs
import kotlin.math.hypot

private const val TRACK_MAP_LOOP_GAP_MULTIPLIER = 6f
private const val TRACK_MAP_TELEMETRY_LOOP_MIN_COVERAGE = 0.96f
private const val TRACK_MAP_TELEMETRY_LOOP_SEAM_EPSILON = 0.035f
private const val TRACK_MAP_TELEMETRY_GAP_MIN_FRACTION = 0.12f
private const val TRACK_MAP_TELEMETRY_GAP_MAX_MEDIAN_FRACTION = 0.10f
private const val TRACK_MAP_TELEMETRY_GAP_MULTIPLIER = 4f
private const val TRACK_MAP_POINTER_JUMP_GAP_MULTIPLIER = 8f
private const val TRACK_MAP_POINTER_MIN_GAP_DISTANCE_MULTIPLIER = 1.8f

@Immutable
internal data class SessionAnalysisTrackMapTraceLookup(
    val breakIndices: ImmutableSet<Int> = persistentSetOf(),
    val jumpThresholdBasePx: Float = 0f,
    val closureThresholdBasePx: Float = 0f,
    val closureDistancePx: Float = Float.POSITIVE_INFINITY,
    val coversTelemetryLoop: Boolean = false,
)

internal fun List<SessionAnalysisFractionPointUi>.buildTraceLookup(): SessionAnalysisTrackMapTraceLookup {
    if (isEmpty()) return SessionAnalysisTrackMapTraceLookup()

    val offsets = toOffsets()
    val firstOffset = offsets.firstOrNull()
    val lastOffset = offsets.lastOrNull()
    return SessionAnalysisTrackMapTraceLookup(
        breakIndices = telemetryPathBreakIndices().toImmutableSet(),
        jumpThresholdBasePx = offsets.computeJumpThreshold(
            gapMultiplier = TRACK_MAP_POINTER_JUMP_GAP_MULTIPLIER,
            minimumGapPx = 0f,
        ),
        closureThresholdBasePx = offsets.computeJumpThreshold(
            gapMultiplier = TRACK_MAP_LOOP_GAP_MULTIPLIER,
            minimumGapPx = 0f,
        ),
        closureDistancePx = if (firstOffset != null && lastOffset != null) {
            hypot(lastOffset.x - firstOffset.x, lastOffset.y - firstOffset.y)
        } else {
            Float.POSITIVE_INFINITY
        },
        coversTelemetryLoop = coversTelemetryLoop(),
    )
}

internal fun SessionAnalysisTrackMapTraceLookup.resolveJumpThreshold(maxDistancePx: Float?): Float? {
    val distance = maxDistancePx ?: return null
    return maxOf(distance * TRACK_MAP_POINTER_MIN_GAP_DISTANCE_MULTIPLIER, jumpThresholdBasePx)
}

internal fun SessionAnalysisTrackMapTraceLookup.isClosedTelemetryLoop(minimumClosurePx: Float = 0f): Boolean =
    coversTelemetryLoop &&
        closureDistancePx.isFinite() &&
        closureDistancePx <= maxOf(minimumClosurePx, closureThresholdBasePx)

internal fun List<SessionAnalysisFractionPointUi>.nearestToFraction(
    fraction: Float?,
): SessionAnalysisFractionPointUi? {
    if (isEmpty() || fraction == null) return null
    return minByOrNull { point -> circularFractionDistance(point.fraction, fraction) }
}

internal fun List<SessionAnalysisFractionPointUi>.nearestToFrameId(frameId: Long?): SessionAnalysisFractionPointUi? {
    if (isEmpty() || frameId == null) return null
    return firstOrNull { point -> point.frameId == frameId }
}

internal fun List<SessionAnalysisFractionPointUi>.nearestIndexToFraction(fraction: Float?): Int? {
    if (isEmpty() || fraction == null) return null
    return indices.minByOrNull { index -> circularFractionDistance(this[index].fraction, fraction) }
}

internal fun List<SessionAnalysisFractionPointUi>.indexOfFrameId(frameId: Long?): Int? {
    if (isEmpty() || frameId == null) return null
    val resolvedIndex = indexOfFirst { point -> point.frameId == frameId }
    return resolvedIndex.takeIf { it >= 0 }
}

internal fun SessionAnalysisFractionPointUi.toOffset(): Offset = Offset(x = x, y = y)

internal fun Float.normalizeTrackFraction(): Float {
    var normalized = this % 1f
    if (normalized < 0f) normalized += 1f
    return normalized
}

internal fun List<SessionAnalysisFractionPointUi>.toOffsets(): List<Offset> = map(
    SessionAnalysisFractionPointUi::toOffset,
)

internal fun List<SessionAnalysisFractionPointUi>.samplePointAtFraction(
    fraction: Float,
): SessionAnalysisFractionPointUi? {
    if (isEmpty()) return null
    if (size == 1) return first()

    val clampedFraction = fraction.coerceIn(0f, 1f)
    val nextIndex = indexOfFirst { point -> point.fraction >= clampedFraction }
        .let { index -> if (index == -1) lastIndex else index }
    if (nextIndex <= 0) return first()

    val previous = this[nextIndex - 1]
    val next = this[nextIndex]
    val span = (next.fraction - previous.fraction).takeIf { it > 0.0001f } ?: return next
    val localFraction = ((clampedFraction - previous.fraction) / span).coerceIn(0f, 1f)
    return SessionAnalysisFractionPointUi(
        fraction = clampedFraction,
        x = lerp(previous.x, next.x, localFraction),
        y = lerp(previous.y, next.y, localFraction),
    )
}

internal fun List<SessionAnalysisFractionPointUi>.sampleDirectionAtFraction(fraction: Float): Offset? {
    if (size < 2) return null
    val clampedFraction = fraction.coerceIn(0f, 1f)
    if (clampedFraction <= first().fraction + 0.0001f) {
        return sampleForwardDirection(startIndex = 0)
    }
    if (clampedFraction >= last().fraction - 0.0001f) {
        return sampleBackwardDirection(startIndex = lastIndex)
    }
    val centerIndex = indexOfFirst { point -> point.fraction >= clampedFraction }
        .let { index -> if (index == -1) lastIndex else index }
    return sampleDirectionAtIndex(index = centerIndex)
}

internal fun List<SessionAnalysisFractionPointUi>.sampleDirectionAtIndex(index: Int?): Offset? {
    if (size < 2 || index == null) return null
    val centerIndex = index.coerceIn(0, lastIndex)
    val closedLoop = isClosedTelemetryLoop()
    val center = this[centerIndex].toOffset()
    val previous = findDistinctOffset(
        startIndex = centerIndex,
        step = -1,
        closedLoop = closedLoop,
        anchor = center,
    ) ?: this[
        when {
            centerIndex > 0 -> centerIndex - 1
            closedLoop -> lastIndex
            else -> 0
        },
    ].toOffset()
    val next = findDistinctOffset(
        startIndex = centerIndex,
        step = 1,
        closedLoop = closedLoop,
        anchor = center,
    ) ?: this[
        when {
            centerIndex < lastIndex -> centerIndex + 1
            closedLoop -> 0
            else -> lastIndex
        },
    ].toOffset()
    return (next - previous).normalizedOrNull()
}

internal fun List<SessionAnalysisFractionPointUi>.isClosedLoop(minimumClosurePx: Float = 0f): Boolean =
    toOffsets().isClosedLoop(minimumClosurePx = minimumClosurePx)

internal fun List<SessionAnalysisFractionPointUi>.isClosedTelemetryLoop(minimumClosurePx: Float = 0f): Boolean =
    coversTelemetryLoop() && isClosedLoop(minimumClosurePx = minimumClosurePx)

internal fun List<SessionAnalysisFractionPointUi>.telemetryPathBreakIndices(): Set<Int> {
    if (size < 3) return emptySet()
    val gaps = buildList {
        for (index in 1 until this@telemetryPathBreakIndices.size) {
            val previousFraction = this@telemetryPathBreakIndices[index - 1].fraction
            val currentFraction = this@telemetryPathBreakIndices[index].fraction
            add(
                forwardTrackFractionGap(
                    startFraction = previousFraction,
                    endFraction = currentFraction,
                ),
            )
        }
    }
    val medianGap = gaps.filter { gap -> gap > 0.0001f }.medianOrNull() ?: return emptySet()
    if (medianGap >= TRACK_MAP_TELEMETRY_GAP_MAX_MEDIAN_FRACTION) return emptySet()
    val threshold = maxOf(
        TRACK_MAP_TELEMETRY_GAP_MIN_FRACTION,
        medianGap * TRACK_MAP_TELEMETRY_GAP_MULTIPLIER,
    )
    return gaps
        .mapIndexedNotNull { index, gap -> (index + 1).takeIf { gap > threshold } }
        .toSet()
}

internal fun List<Offset>.isClosedLoop(
    minimumClosurePx: Float = 0f,
    gapMultiplier: Float = TRACK_MAP_LOOP_GAP_MULTIPLIER,
): Boolean {
    if (size < 3) return false
    val closureDistance = hypot(last().x - first().x, last().y - first().y)
    if (!closureDistance.isFinite()) return false
    val jumpThreshold = computeJumpThreshold(
        gapMultiplier = gapMultiplier,
        minimumGapPx = minimumClosurePx,
    )
    return closureDistance <= jumpThreshold
}

internal fun List<Offset>.computeJumpThreshold(gapMultiplier: Float, minimumGapPx: Float): Float {
    val segmentLengths = buildList {
        for (index in 1 until this@computeJumpThreshold.size) {
            val previous = this@computeJumpThreshold[index - 1]
            val current = this@computeJumpThreshold[index]
            val length = hypot(current.x - previous.x, current.y - previous.y)
            if (length.isFinite() && length > 0.5f) add(length)
        }
    }
    val median = segmentLengths.medianOrNull() ?: 0f
    return maxOf(minimumGapPx, median * gapMultiplier)
}

internal fun Offset.normalizedOrNull(): Offset? {
    val length = hypot(x, y)
    if (!length.isFinite() || length <= 0.0001f) return null
    return Offset(x / length, y / length)
}

internal fun Offset.distanceSquaredTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return dx * dx + dy * dy
}

internal fun List<Float>.medianOrNull(): Float? {
    if (isEmpty()) return null
    val sorted = sorted()
    val middleIndex = sorted.lastIndex / 2
    return if (sorted.size % 2 == 0) {
        (sorted[middleIndex] + sorted[middleIndex + 1]) * 0.5f
    } else {
        sorted[middleIndex]
    }
}

internal fun projectPointOnSegment(pointer: Offset, start: Offset, end: Offset): Float {
    val segmentX = end.x - start.x
    val segmentY = end.y - start.y
    val lengthSquared = segmentX * segmentX + segmentY * segmentY
    if (lengthSquared <= 0.0001f) return 0f

    val projection = ((pointer.x - start.x) * segmentX + (pointer.y - start.y) * segmentY) / lengthSquared
    return projection.coerceIn(0f, 1f)
}

internal fun circularFractionDistance(a: Float, b: Float): Float {
    val direct = abs(a - b)
    return minOf(direct, 1f - direct)
}

internal fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

private fun List<SessionAnalysisFractionPointUi>.findDistinctOffset(
    startIndex: Int,
    step: Int,
    closedLoop: Boolean,
    anchor: Offset,
): Offset? {
    if (isEmpty()) return null
    var currentIndex = startIndex
    repeat(lastIndex) {
        currentIndex += step
        if (closedLoop) {
            currentIndex = when {
                currentIndex < 0 -> lastIndex
                currentIndex > lastIndex -> 0
                else -> currentIndex
            }
        } else if (currentIndex !in indices) {
            return null
        }

        val candidate = this[currentIndex].toOffset()
        if ((candidate - anchor).getDistance() > 0.0001f) {
            return candidate
        }
    }
    return null
}

private fun List<SessionAnalysisFractionPointUi>.coversTelemetryLoop(): Boolean {
    if (size < 3) return false
    val firstFraction = first().fraction.coerceIn(0f, 1f)
    val lastFraction = last().fraction.coerceIn(0f, 1f)
    val orderedCoverage = if (lastFraction >= firstFraction) {
        lastFraction - firstFraction
    } else {
        lastFraction + 1f - firstFraction
    }
    val touchesStartFinish = firstFraction <= TRACK_MAP_TELEMETRY_LOOP_SEAM_EPSILON &&
        lastFraction >= 1f - TRACK_MAP_TELEMETRY_LOOP_SEAM_EPSILON
    return touchesStartFinish || orderedCoverage >= TRACK_MAP_TELEMETRY_LOOP_MIN_COVERAGE
}

private fun forwardTrackFractionGap(startFraction: Float, endFraction: Float): Float {
    val start = startFraction.normalizeTrackFraction()
    val end = endFraction.normalizeTrackFraction()
    return if (end >= start) {
        end - start
    } else {
        end + 1f - start
    }
}

private fun List<SessionAnalysisFractionPointUi>.sampleForwardDirection(startIndex: Int): Offset? {
    val start = getOrNull(startIndex)?.toOffset() ?: return null
    val next = findDistinctOffset(
        startIndex = startIndex,
        step = 1,
        closedLoop = false,
        anchor = start,
    ) ?: return null
    return (next - start).normalizedOrNull()
}

private fun List<SessionAnalysisFractionPointUi>.sampleBackwardDirection(startIndex: Int): Offset? {
    val start = getOrNull(startIndex)?.toOffset() ?: return null
    val previous = findDistinctOffset(
        startIndex = startIndex,
        step = -1,
        closedLoop = false,
        anchor = start,
    ) ?: return null
    return (start - previous).normalizedOrNull()
}
