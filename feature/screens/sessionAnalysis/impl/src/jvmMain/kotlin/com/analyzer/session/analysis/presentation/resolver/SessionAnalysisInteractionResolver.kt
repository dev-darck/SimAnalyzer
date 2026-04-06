package com.analyzer.session.analysis.presentation.resolver

import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisInteractionState
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.abs

/**
 * Resolves the active sample and comparison point from the current cursor and pinning state.
 */
internal fun SessionAnalysisInteractionState.resolveActivePoint(
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    fallback: SessionAnalysisComparisonPointUi?,
    fraction: Float?,
): SessionAnalysisComparisonPointUi? {
    if (comparisonPoints.isEmpty()) return fallback
    if (fraction == null) return fallback
    val resolvedIndex = comparisonFractions.nearestIndex(fraction)
    return comparisonPoints.getOrNull(resolvedIndex) ?: fallback
}

internal fun SessionAnalysisInteractionState.resolveActiveSample(
    visibleSamples: ImmutableList<SessionAnalysisSampleUi>,
    fallback: SessionAnalysisSampleUi?,
    activePoint: SessionAnalysisComparisonPointUi?,
): SessionAnalysisSampleUi? {
    if (visibleSamples.isEmpty()) return fallback
    val selectedFrameId = activePoint?.selectedFrameId
    if (selectedFrameId != null) {
        val sampleIndex = sampleIndexByFrameId[selectedFrameId]
        if (sampleIndex != null) {
            return visibleSamples.getOrNull(sampleIndex) ?: fallback
        }
    }
    val trackPosition = activePoint?.trackPosition ?: return fallback
    val resolvedIndex = resolvedSamplePositions.nearestIndex(trackPosition)
    return visibleSamples.getOrNull(resolvedIndex) ?: fallback
}

internal fun SessionAnalysisInteractionState.snapFraction(fraction: Float?): Float? {
    if (comparisonFractions.isEmpty()) return fraction
    if (fraction == null) return null
    return comparisonFractions.getOrNull(comparisonFractions.nearestIndex(fraction)) ?: fraction
}

private fun List<Float>.nearestIndex(target: Float): Int {
    if (isEmpty()) return -1
    if (size == 1) return 0
    val insertionIndex = binarySearch(target)
    if (insertionIndex >= 0) return insertionIndex

    val upperIndex = (-insertionIndex - 1).coerceIn(0, lastIndex)
    val lowerIndex = (upperIndex - 1).coerceIn(0, lastIndex)
    if (upperIndex == lowerIndex) return upperIndex

    val upperDistance = abs(this[upperIndex] - target)
    val lowerDistance = abs(this[lowerIndex] - target)
    return if (upperDistance < lowerDistance) upperIndex else lowerIndex
}
