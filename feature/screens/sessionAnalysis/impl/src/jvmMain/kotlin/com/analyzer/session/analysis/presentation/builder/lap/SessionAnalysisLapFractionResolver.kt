package com.analyzer.session.analysis.presentation.builder.lap

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlin.math.hypot

/**
 * Resolves stable lap fractions so charts and track overlays stay aligned even with sparse telemetry.
 */
private const val TELEMETRY_TRACK_WRAP_THRESHOLD: Float = 0.45f
private const val GEOMETRY_TRACK_WRAP_THRESHOLD_FRACTION: Float = 0.22f

internal fun resolveLapFractions(
    samples: List<SessionAnalysisSample>,
    trackMap: SessionAnalysisTrackMap?,
): List<Float> = resolveLapFractionResolution(samples, trackMap).fractions

internal fun resolveTraceLapFractions(
    samples: List<SessionAnalysisSample>,
    trackMap: SessionAnalysisTrackMap?,
): List<Float> {
    if (samples.isEmpty()) return emptyList()
    if (samples.size == 1) return listOf(0f)

    val orderedSamples = samples.sortedBy(SessionAnalysisSample::sampleIndexInLap)
    resolveGeometryFractions(
        samples = orderedSamples,
        trackPoints = trackMap?.points.orEmpty(),
    )?.let { return it.fractions }
    resolveDistanceFractions(orderedSamples)?.let { return it }
    resolveTelemetryFractions(orderedSamples)?.let { return it.fractions }
    return orderedSamples.indices.map { index ->
        index.toFloat() / orderedSamples.lastIndex.coerceAtLeast(1).toFloat()
    }
}

internal fun resolveLapFractionResolution(
    samples: List<SessionAnalysisSample>,
    trackMap: SessionAnalysisTrackMap?,
): SessionAnalysisLapFractionResolution {
    if (samples.isEmpty()) return SessionAnalysisLapFractionResolution(fractions = emptyList())
    if (samples.size == 1) {
        return SessionAnalysisLapFractionResolution(fractions = listOf(0f))
    }

    val orderedSamples = samples.sortedBy(SessionAnalysisSample::sampleIndexInLap)
    resolveTelemetryFractions(orderedSamples)?.let { return it.toResolution() }
    resolveGeometryFractions(
        samples = orderedSamples,
        trackPoints = trackMap?.points.orEmpty(),
    )?.let { return it.toResolution() }
    resolveDistanceFractions(orderedSamples)?.let { fractions ->
        return SessionAnalysisLapFractionResolution(fractions = fractions)
    }

    return SessionAnalysisLapFractionResolution(
        fractions = orderedSamples.indices.map { index ->
            index.toFloat() / orderedSamples.lastIndex.coerceAtLeast(1).toFloat()
        },
    )
}

private fun resolveTelemetryFractions(samples: List<SessionAnalysisSample>): ResolvedLapProgress? {
    val rawPositions = samples.map { sample ->
        sample.trackPosition
            ?.takeIf(Float::isFinite)
            ?.coerceIn(0f, 1f)
    }
    val usableValues = rawPositions.filterNotNull()
    if (usableValues.size < 2) return null

    val progress = unwrapTelemetryFractions(rawPositions)
    val monotonic = progress.fractions

    val spread = (usableValues.maxOrNull() ?: 0f) - (usableValues.minOrNull() ?: 0f)
    val monotonicSpread = monotonic.last() - monotonic.first()
    val increasingSteps = monotonic.zipWithNext().count { (start, end) ->
        end - start > 0.0005f
    }
    val hasUsableTelemetryFractions = spread >= 0.002f &&
        monotonicSpread >= 0.002f &&
        increasingSteps >= 1

    if (!hasUsableTelemetryFractions) return null
    return if (progress.wrapStartIndex != null) {
        ResolvedLapProgress(
            fractions = normalizeProgressFractions(monotonic),
            wrapStartIndex = progress.wrapStartIndex,
        )
    } else {
        progress
    }
}

private fun resolveGeometryFractions(
    samples: List<SessionAnalysisSample>,
    trackPoints: List<SessionAnalysisTrackMapPoint>,
): ResolvedLapProgress? {
    if (samples.size < 2 || trackPoints.size < 2) return null

    val cumulativeDistances = trackPoints.cumulativeDistances()
    val totalDistance = cumulativeDistances.lastOrNull()?.takeIf { it > 0.001f } ?: return null
    val searchWindow = GeometrySearchWindow(
        forward = maxOf(96, trackPoints.size / 18),
        backward = maxOf(18, trackPoints.size / 160),
    )
    val resolvedDistances = MutableList(samples.size) { 0f }

    var lastMatchedIndex: Int? = null
    var lastMatchedDistance = 0f
    var wrapDistanceOffset = 0f
    var wrappedForward = false
    var wrapStartIndex: Int? = null
    samples.forEachIndexed { index, sample ->
        val sampleX = sample.trackX?.takeIf(Float::isFinite)
        val sampleY = sample.trackY?.takeIf(Float::isFinite)
        if (sampleX == null || sampleY == null) {
            resolvedDistances[index] = resolvedDistances.getOrElse(index - 1) { 0f }
            return@forEachIndexed
        }

        val matchedIndex = trackPoints.resolveMatchedTrackPointIndex(
            x = sampleX,
            y = sampleY,
            previousIndex = lastMatchedIndex,
            searchWindow = searchWindow,
        )

        lastMatchedIndex = matchedIndex
        val matchedDistance = cumulativeDistances[matchedIndex]
        if (
            index > 0 &&
            matchedDistance + totalDistance * GEOMETRY_TRACK_WRAP_THRESHOLD_FRACTION < lastMatchedDistance
        ) {
            wrapDistanceOffset += totalDistance
            wrappedForward = true
            if (wrapStartIndex == null) {
                wrapStartIndex = index
            }
        }
        lastMatchedDistance = matchedDistance
        resolvedDistances[index] = matchedDistance + wrapDistanceOffset
    }

    val resolvedFractions = if (wrappedForward) {
        normalizeProgressFractions(resolvedDistances)
    } else {
        resolvedDistances.map { distance -> (distance / totalDistance).coerceIn(0f, 1f) }
    }
    val spread = resolvedFractions.last() - resolvedFractions.first()
    return ResolvedLapProgress(
        fractions = resolvedFractions,
        wrapStartIndex = wrapStartIndex,
    ).takeIf { spread >= 0.01f }
}

private fun List<SessionAnalysisTrackMapPoint>.resolveMatchedTrackPointIndex(
    x: Float,
    y: Float,
    previousIndex: Int?,
    searchWindow: GeometrySearchWindow,
): Int {
    previousIndex ?: return nearestIndexTo(x, y)

    val windowStart = (previousIndex - searchWindow.backward).coerceAtLeast(0)
    val windowEnd = (previousIndex + searchWindow.forward).coerceAtMost(lastIndex)
    val windowIndex = nearestIndexTo(
        x = x,
        y = y,
        startIndex = windowStart,
        endIndex = windowEnd,
    )
    val globalIndex = nearestIndexTo(x, y)
    val seamWindow = maxOf(searchWindow.forward, searchWindow.backward)
    return if (previousIndex >= lastIndex - seamWindow && globalIndex <= seamWindow) {
        globalIndex
    } else {
        windowIndex.coerceAtLeast(previousIndex)
    }
}

private fun unwrapTelemetryFractions(rawPositions: List<Float?>): ResolvedLapProgress {
    val resolved = MutableList(rawPositions.size) { 0f }
    var lastResolved = 0f
    var lastRaw: Float? = null
    var lapOffset = 0f
    var wrapStartIndex: Int? = null
    rawPositions.forEachIndexed { index, value ->
        val raw = value ?: lastRaw ?: 0f
        val previousRaw = lastRaw
        if (
            index > 0 &&
            previousRaw != null &&
            raw - previousRaw <= -TELEMETRY_TRACK_WRAP_THRESHOLD
        ) {
            lapOffset += 1f
            if (wrapStartIndex == null) {
                wrapStartIndex = index
            }
        }
        val unwrapped = (raw + lapOffset).coerceAtLeast(lastResolved)
        resolved[index] = unwrapped
        lastResolved = unwrapped
        lastRaw = raw
    }
    return ResolvedLapProgress(
        fractions = resolved,
        wrapStartIndex = wrapStartIndex,
    )
}

private fun normalizeProgressFractions(fractions: List<Float>): List<Float> {
    if (fractions.isEmpty()) return emptyList()
    val start = fractions.first()
    val end = fractions.last()
    val span = (end - start).takeIf { it > 0.0001f } ?: return List(fractions.size) { 0f }
    return fractions.map { fraction ->
        ((fraction - start) / span).coerceIn(0f, 1f)
    }
}

private fun ResolvedLapProgress.toResolution(): SessionAnalysisLapFractionResolution {
    val trailStartFraction = wrapStartIndex
        ?.takeIf { index -> index in 1 until fractions.lastIndex }
        ?.let(fractions::get)
    return SessionAnalysisLapFractionResolution(
        fractions = fractions,
        trailStartFraction = trailStartFraction,
    )
}

private fun List<SessionAnalysisTrackMapPoint>.nearestIndexTo(
    x: Float,
    y: Float,
    startIndex: Int = 0,
    endIndex: Int = lastIndex,
): Int {
    if (isEmpty()) return 0
    var bestIndex = startIndex.coerceIn(0, lastIndex)
    var bestDistance = Float.POSITIVE_INFINITY
    val clampedStart = startIndex.coerceIn(0, lastIndex)
    val clampedEnd = endIndex.coerceIn(clampedStart, lastIndex)
    for (index in clampedStart..clampedEnd) {
        val point = this[index]
        val distance = squaredDistance(
            ax = point.x,
            ay = point.y,
            bx = x,
            by = y,
        )
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex
}

private fun List<SessionAnalysisTrackMapPoint>.cumulativeDistances(): List<Float> {
    if (isEmpty()) return emptyList()
    val result = MutableList(size) { 0f }
    var total = 0f
    for (index in 1 until size) {
        val previous = this[index - 1]
        val current = this[index]
        val segmentLength = hypot(current.x - previous.x, current.y - previous.y)
        if (segmentLength.isFinite() && segmentLength > 0.0001f) {
            total += segmentLength
        }
        result[index] = total
    }
    return result
}

/**
 * Computes fractions from the cumulative XY distance of the samples themselves.
 * Works even when no track map is available, as long as samples have finite coordinates.
 */
private fun resolveDistanceFractions(samples: List<SessionAnalysisSample>): List<Float>? {
    if (samples.size < 2) return null

    val xyPresent = samples.count { s ->
        s.trackX?.isFinite() == true && s.trackY?.isFinite() == true
    }
    if (xyPresent < samples.size / 2) return null

    val cumulativeDistances = MutableList(samples.size) { 0f }
    var total = 0f
    var prevX = samples[0].trackX ?: 0f
    var prevY = samples[0].trackY ?: 0f

    for (i in 1 until samples.size) {
        val sx = samples[i].trackX?.takeIf(Float::isFinite) ?: prevX
        val sy = samples[i].trackY?.takeIf(Float::isFinite) ?: prevY
        val seg = hypot(sx - prevX, sy - prevY)
        if (seg.isFinite()) total += seg
        cumulativeDistances[i] = total
        prevX = sx
        prevY = sy
    }

    if (total < 10f) return null // Too short to be meaningful

    return cumulativeDistances.map { d -> (d / total).coerceIn(0f, 1f) }
}

private fun squaredDistance(ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = ax - bx
    val dy = ay - by
    return dx * dx + dy * dy
}
