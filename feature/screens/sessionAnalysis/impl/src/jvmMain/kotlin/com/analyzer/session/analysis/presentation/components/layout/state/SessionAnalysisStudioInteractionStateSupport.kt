package com.analyzer.session.analysis.presentation.components.layout.state

import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi

/**
 * Interaction helpers keep hover, pinning, and scroll-driven focus rules consistent across layout panes.
 */
internal const val focusCursorSmoothingDurationMs: Int = 120
internal const val focusMapHoverDeadbandFraction: Float = 0.0045f

internal fun Iterable<SessionAnalysisComparisonPointUi>.nearestToTrackPosition(
    trackPosition: Float,
): SessionAnalysisComparisonPointUi? = minByOrNull { point ->
    circularFractionDistance(point.trackPosition, trackPosition)
}

internal fun List<SessionAnalysisComparisonPointUi>.resolveActivePointFallback(
    fallback: SessionAnalysisComparisonPointUi?,
    fraction: Float?,
): SessionAnalysisComparisonPointUi? {
    if (isEmpty()) return fallback
    if (fraction == null) return fallback
    return minByOrNull { point -> circularFractionDistance(point.fraction, fraction) } ?: fallback
}

internal fun List<SessionAnalysisSampleUi>.resolveActiveSampleFallback(
    fallback: SessionAnalysisSampleUi?,
    activePoint: SessionAnalysisComparisonPointUi?,
): SessionAnalysisSampleUi? {
    if (isEmpty()) return fallback
    val selectedFrameId = activePoint?.selectedFrameId
    if (selectedFrameId != null) {
        return firstOrNull { sample -> sample.frameId == selectedFrameId } ?: fallback
    }
    val trackPosition = activePoint?.trackPosition ?: return fallback
    return minByOrNull { sample ->
        circularFractionDistance(sample.trackPosition ?: trackPosition, trackPosition)
    } ?: fallback
}

internal fun circularFractionDistance(left: Float, right: Float): Float {
    val directDistance = kotlin.math.abs(left - right)
    return minOf(directDistance, 1f - directDistance)
}

internal fun shouldUpdateMapHover(
    currentFraction: Float?,
    currentFrameId: Long?,
    nextFraction: Float?,
    nextFrameId: Long?,
    focusMode: Boolean,
): Boolean {
    if (currentFraction == nextFraction && currentFrameId == nextFrameId) return false
    if (shouldRetainFocusHoverOnExit(focusMode = focusMode, nextFraction = nextFraction, nextFrameId = nextFrameId)) {
        return false
    }
    if (nextFraction == null && nextFrameId == null) return true
    if (!focusMode) return true
    if (nextFrameId != null && currentFrameId == nextFrameId) return false

    val resolvedCurrentFraction = currentFraction ?: return true
    val resolvedNextFraction = nextFraction ?: return true
    return circularFractionDistance(resolvedCurrentFraction, resolvedNextFraction) >= focusMapHoverDeadbandFraction
}

internal fun shouldRetainFocusHoverOnExit(focusMode: Boolean, nextFraction: Float?, nextFrameId: Long?): Boolean =
    focusMode && nextFraction == null && nextFrameId == null
