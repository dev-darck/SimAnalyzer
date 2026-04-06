package com.analyzer.session.analysis.presentation.components.map.support

import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapHit
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapHitSearchOptions
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPointerState
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlin.math.hypot

private const val TRACK_MAP_MIN_SEGMENT_LENGTH = 0.0001f
private const val TRACK_MAP_FOCUS_PREFERRED_HIT_DISTANCE_MULTIPLIER = 2f
private const val TRACK_MAP_NORMAL_PREFERRED_HIT_DISTANCE_MULTIPLIER = 2.8f

internal fun resolveTrackMapPointerTarget(
    context: SessionAnalysisTrackMapPointerState,
    pointer: Offset,
    purpose: SessionAnalysisTrackMapPointerPurpose,
): Pair<Float?, Long?> = if (context.selectedTraceAvailable) {
    context.projectedSelectedTrace.resolveSelectedPointerTarget(
        pointer = pointer,
        traceLookup = context.projectedSelectedTraceLookup,
        options = SelectedPointerTargetOptions(
            hoverDistancePx = context.hoverDistancePx,
            hoverPreferenceIndex = context.hoverPreferenceIndex,
            hoverLocalIndexWindow = context.hoverLocalIndexWindow,
            focusMode = context.focusMode,
            purpose = purpose,
        ),
    )
} else {
    context.hoverTrace.resolveFallbackPointerTarget(
        pointer = pointer,
        traceLookup = context.hoverTraceLookup,
        options = FallbackPointerTargetOptions(
            hoverDistancePx = context.hoverDistancePx,
            hoverPreferenceFraction = context.hoverPreferenceFraction,
            focusMode = context.focusMode,
            purpose = purpose,
        ),
    )
}

private fun List<SessionAnalysisFractionPointUi>.resolveSelectedPointerTarget(
    pointer: Offset,
    traceLookup: SessionAnalysisTrackMapTraceLookup,
    options: SelectedPointerTargetOptions,
): Pair<Float?, Long?> {
    val directHit = findNearestHit(
        pointer = pointer,
        traceLookup = traceLookup,
        maxDistancePx = options.hoverDistancePx,
        ignoreJumpSegments = true,
    )
    val preferredHit = findNearestHit(
        pointer = pointer,
        traceLookup = traceLookup,
        maxDistancePx = options.preferredHitDistancePx(),
        preferredIndex = options.hoverPreferenceIndex,
        indexWindow = options.hoverLocalIndexWindow,
        ignoreJumpSegments = true,
    )
    val fallbackHit = findNearestHit(pointer = pointer, traceLookup = traceLookup).takeIf {
        options.purpose == SessionAnalysisTrackMapPointerPurpose.Press || options.hoverPreferenceIndex == null
    }
    val resolved = when {
        options.focusMode && options.purpose == SessionAnalysisTrackMapPointerPurpose.Hover ->
            preferredHit ?: directHit ?: fallbackHit

        else -> directHit ?: preferredHit ?: fallbackHit
    }
    return resolved?.let { hit -> hit.fraction to hit.target.frameId } ?: (null to null)
}

private fun List<SessionAnalysisFractionPointUi>.resolveFallbackPointerTarget(
    pointer: Offset,
    traceLookup: SessionAnalysisTrackMapTraceLookup,
    options: FallbackPointerTargetOptions,
): Pair<Float?, Long?> {
    val directHit = findNearestHit(
        pointer = pointer,
        traceLookup = traceLookup,
        maxDistancePx = options.hoverDistancePx,
        ignoreJumpSegments = true,
    )
    val preferredHit = findNearestHit(
        pointer = pointer,
        traceLookup = traceLookup,
        maxDistancePx = options.preferredHitDistancePx(),
        preferredFraction = options.hoverPreferenceFraction,
        fractionWindow = if (options.focusMode) 0.035f else 0.08f,
        ignoreJumpSegments = true,
    )
    val fallbackHit = findNearestHit(pointer = pointer, traceLookup = traceLookup).takeIf {
        options.purpose == SessionAnalysisTrackMapPointerPurpose.Press || options.hoverPreferenceFraction == null
    }
    val resolved = directHit ?: preferredHit ?: fallbackHit
    return resolved?.let { hit -> hit.fraction to hit.target.frameId } ?: (null to null)
}

private fun SelectedPointerTargetOptions.preferredHitDistancePx(): Float = hoverDistancePx * if (focusMode) {
    TRACK_MAP_FOCUS_PREFERRED_HIT_DISTANCE_MULTIPLIER
} else {
    TRACK_MAP_NORMAL_PREFERRED_HIT_DISTANCE_MULTIPLIER
}

private fun FallbackPointerTargetOptions.preferredHitDistancePx(): Float = hoverDistancePx * if (focusMode) {
    TRACK_MAP_FOCUS_PREFERRED_HIT_DISTANCE_MULTIPLIER
} else {
    TRACK_MAP_NORMAL_PREFERRED_HIT_DISTANCE_MULTIPLIER
}

private fun List<SessionAnalysisFractionPointUi>.findNearestHit(
    pointer: Offset,
    traceLookup: SessionAnalysisTrackMapTraceLookup,
    maxDistancePx: Float? = null,
    preferredFraction: Float? = null,
    fractionWindow: Float? = null,
    preferredIndex: Int? = null,
    indexWindow: Int? = null,
    ignoreJumpSegments: Boolean = false,
): SessionAnalysisTrackMapHit? {
    if (isEmpty()) return null

    val maxDistanceSquared = maxDistancePx?.let { it * it } ?: Float.POSITIVE_INFINITY
    if (size == 1) {
        return resolveSinglePointHit(
            pointer = pointer,
            maxDistanceSquared = maxDistanceSquared,
            preferredFraction = preferredFraction,
            fractionWindow = fractionWindow,
        )
    }

    val searchOptions = SessionAnalysisTrackMapHitSearchOptions(
        maxDistanceSquared = maxDistanceSquared,
        preferredFraction = preferredFraction,
        fractionWindow = fractionWindow,
        preferredIndex = preferredIndex,
        indexWindow = indexWindow,
        jumpThreshold = if (ignoreJumpSegments) traceLookup.resolveJumpThreshold(maxDistancePx) else null,
    )
    return findNearestSegmentHit(
        pointer = pointer,
        options = searchOptions,
        traceLookup = traceLookup,
    )
}

private fun List<SessionAnalysisFractionPointUi>.resolveSinglePointHit(
    pointer: Offset,
    maxDistanceSquared: Float,
    preferredFraction: Float?,
    fractionWindow: Float?,
): SessionAnalysisTrackMapHit? {
    val onlyPoint = first()
    val hit = SessionAnalysisTrackMapHit(
        fraction = onlyPoint.fraction,
        target = onlyPoint,
        distanceSquared = pointer.distanceSquaredTo(onlyPoint.toOffset()),
    )
    val matchesPreferredFraction = preferredFraction == null ||
        fractionWindow == null ||
        circularFractionDistance(onlyPoint.fraction, preferredFraction) <= fractionWindow
    return hit.takeIf { it.distanceSquared <= maxDistanceSquared && matchesPreferredFraction }
}

private fun List<SessionAnalysisFractionPointUi>.findNearestSegmentHit(
    pointer: Offset,
    options: SessionAnalysisTrackMapHitSearchOptions,
    traceLookup: SessionAnalysisTrackMapTraceLookup,
): SessionAnalysisTrackMapHit? {
    val segmentRange = resolveSegmentSearchRange(
        preferredIndex = options.preferredIndex,
        indexWindow = options.indexWindow,
    )

    var bestHit: SessionAnalysisTrackMapHit? = null
    for (index in segmentRange) {
        val start = this[index - 1]
        val end = this[index]
        if (!isTrackMapHitCandidateUsable(index, start, end, options, traceLookup.breakIndices)) continue

        val hit = buildTrackMapHit(pointer = pointer, start = start, end = end)
        val isCloserHit = bestHit == null || hit.distanceSquared < bestHit.distanceSquared
        if (hit.distanceSquared <= options.maxDistanceSquared && isCloserHit) {
            bestHit = hit
        }
    }

    if (shouldSearchClosureSegment(options.preferredIndex, options.indexWindow, lastIndex)) {
        val closureHit = findClosureSegmentHit(
            pointer = pointer,
            options = options,
            traceLookup = traceLookup,
        )
        if (closureHit != null && (bestHit == null || closureHit.distanceSquared < bestHit.distanceSquared)) {
            bestHit = closureHit
        }
    }

    return bestHit
}

private fun isTrackMapHitCandidateUsable(
    index: Int,
    start: SessionAnalysisFractionPointUi,
    end: SessionAnalysisFractionPointUi,
    options: SessionAnalysisTrackMapHitSearchOptions,
    telemetryBreakIndices: Set<Int>,
): Boolean = index !in telemetryBreakIndices &&
    isTrackMapSegmentUsable(start = start, end = end, jumpThreshold = options.jumpThreshold) &&
    matchesPreferredFractionWindow(start, end, options.preferredFraction, options.fractionWindow)

private fun List<SessionAnalysisFractionPointUi>.resolveSegmentSearchRange(
    preferredIndex: Int?,
    indexWindow: Int?,
): IntRange {
    if (preferredIndex == null || indexWindow == null) return 1..lastIndex
    val centerIndex = preferredIndex.coerceIn(0, lastIndex)
    val startIndex = (centerIndex - indexWindow).coerceAtLeast(1)
    val endIndex = (centerIndex + indexWindow).coerceAtMost(lastIndex)
    return startIndex..endIndex
}

private fun shouldSearchClosureSegment(preferredIndex: Int?, indexWindow: Int?, lastIndex: Int): Boolean {
    if (preferredIndex == null || indexWindow == null) return true
    return preferredIndex <= indexWindow || preferredIndex + indexWindow >= lastIndex
}

private fun List<SessionAnalysisFractionPointUi>.findClosureSegmentHit(
    pointer: Offset,
    options: SessionAnalysisTrackMapHitSearchOptions,
    traceLookup: SessionAnalysisTrackMapTraceLookup,
): SessionAnalysisTrackMapHit? {
    if (!traceLookup.isClosedTelemetryLoop(minimumClosurePx = options.jumpThreshold ?: 0f)) return null

    val start = last()
    val end = first()
    if (!isTrackMapSegmentUsable(start = start, end = end, jumpThreshold = options.jumpThreshold)) return null
    if (!matchesPreferredFractionWindow(start, end, options.preferredFraction, options.fractionWindow)) return null

    val localFraction = projectPointOnSegment(
        pointer = pointer,
        start = start.toOffset(),
        end = end.toOffset(),
    )
    val nearestPoint = Offset(
        x = lerp(start.x, end.x, localFraction),
        y = lerp(start.y, end.y, localFraction),
    )
    val target = if (localFraction < 0.5f) start else end
    val hit = SessionAnalysisTrackMapHit(
        fraction = interpolateTrackFraction(start.fraction, end.fraction, localFraction),
        target = target,
        distanceSquared = pointer.distanceSquaredTo(nearestPoint),
    )
    return hit.takeIf { it.distanceSquared <= options.maxDistanceSquared }
}

private fun isTrackMapSegmentUsable(
    start: SessionAnalysisFractionPointUi,
    end: SessionAnalysisFractionPointUi,
    jumpThreshold: Float?,
): Boolean {
    val segmentLength = hypot(end.x - start.x, end.y - start.y)
    if (!segmentLength.isFinite() || segmentLength <= TRACK_MAP_MIN_SEGMENT_LENGTH) return false
    return jumpThreshold == null || segmentLength <= jumpThreshold
}

private fun matchesPreferredFractionWindow(
    start: SessionAnalysisFractionPointUi,
    end: SessionAnalysisFractionPointUi,
    preferredFraction: Float?,
    fractionWindow: Float?,
): Boolean {
    if (preferredFraction == null || fractionWindow == null) return true
    val midpointFraction = midpointTrackFraction(start.fraction, end.fraction)
    return circularFractionDistance(midpointFraction, preferredFraction) <= fractionWindow
}

private fun midpointTrackFraction(startFraction: Float, endFraction: Float): Float {
    val normalizedStart = startFraction.coerceIn(0f, 1f)
    val normalizedEnd = endFraction.coerceIn(0f, 1f)
    val wrappedDistance = if (normalizedEnd >= normalizedStart) {
        normalizedEnd - normalizedStart
    } else {
        normalizedEnd + 1f - normalizedStart
    }
    val midpoint = normalizedStart + wrappedDistance * 0.5f
    return midpoint.takeIf { it < 1f } ?: (midpoint - 1f)
}

private fun buildTrackMapHit(
    pointer: Offset,
    start: SessionAnalysisFractionPointUi,
    end: SessionAnalysisFractionPointUi,
): SessionAnalysisTrackMapHit {
    val localFraction = projectPointOnSegment(
        pointer = pointer,
        start = start.toOffset(),
        end = end.toOffset(),
    )
    val nearestPoint = Offset(
        x = lerp(start.x, end.x, localFraction),
        y = lerp(start.y, end.y, localFraction),
    )
    return SessionAnalysisTrackMapHit(
        fraction = interpolateTrackFraction(start.fraction, end.fraction, localFraction),
        target = if (localFraction < 0.5f) start else end,
        distanceSquared = pointer.distanceSquaredTo(nearestPoint),
    )
}

private fun interpolateTrackFraction(startFraction: Float, endFraction: Float, localFraction: Float): Float {
    val normalizedStart = startFraction.normalizeTrackFraction()
    val normalizedEnd = endFraction.normalizeTrackFraction()
    val wrappedEnd = if (normalizedEnd < normalizedStart) normalizedEnd + 1f else normalizedEnd
    val interpolated = normalizedStart + ((wrappedEnd - normalizedStart) * localFraction.coerceIn(0f, 1f))
    return interpolated.normalizeTrackFraction()
}
