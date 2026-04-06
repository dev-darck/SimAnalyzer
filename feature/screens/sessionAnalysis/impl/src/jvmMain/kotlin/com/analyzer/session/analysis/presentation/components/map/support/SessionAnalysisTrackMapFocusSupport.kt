package com.analyzer.session.analysis.presentation.components.map.support

import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Focus helpers derive the visible slice of the map when the user locks onto one corner or cursor region.
 */
internal fun ImmutableList<SessionAnalysisFractionPointUi>.buildSelectedTrailTrace(
    trailFraction: Float?,
    trailStartFraction: Float?,
): ImmutableList<SessionAnalysisFractionPointUi> {
    if (isEmpty() || trailFraction == null) return persistentListOf()
    return takeThroughFraction(
        fraction = trailFraction.coerceIn(0f, 1f),
        trailStartFraction = trailStartFraction?.coerceIn(0f, 1f),
    )
}

internal fun ImmutableList<SessionAnalysisFractionPointUi>.resolveTrailMarkerFraction(
    markerFrameId: Long?,
    markerFraction: Float?,
): Float? = nearestToFrameId(markerFrameId)?.fraction ?: markerFraction

private fun ImmutableList<SessionAnalysisFractionPointUi>.takeThroughFraction(
    fraction: Float,
    trailStartFraction: Float?,
): ImmutableList<SessionAnalysisFractionPointUi> {
    if (isEmpty()) return persistentListOf()
    val endIndex = nearestProgressIndexToFraction(fraction) ?: return persistentListOf()
    val startIndex = trailStartFraction
        ?.let(::nearestProgressIndexToFraction)
        ?.takeIf { index -> index in 1..endIndex }
        ?: 0
    return subList(startIndex, endIndex.coerceIn(startIndex, lastIndex) + 1).toImmutableList()
}

private fun ImmutableList<SessionAnalysisFractionPointUi>.nearestProgressIndexToFraction(fraction: Float?): Int? {
    if (isEmpty() || fraction == null) return null
    val clampedFraction = fraction.coerceIn(0f, 1f)
    val nextIndex = indexOfFirst { point -> point.fraction >= clampedFraction }
        .let { index -> if (index == -1) lastIndex else index }
    if (nextIndex <= 0) return 0
    val previousIndex = nextIndex - 1
    val previousDistance = kotlin.math.abs(this[previousIndex].fraction - clampedFraction)
    val nextDistance = kotlin.math.abs(this[nextIndex].fraction - clampedFraction)
    return if (previousDistance <= nextDistance) previousIndex else nextIndex
}
