package com.analyzer.session.analysis.presentation.builder.track.trace

import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint

/**
 * Shared trace helpers smooth and sanitize projected points before the canvas consumes them.
 */
internal fun Float.normalizeTrackFraction(): Float {
    var normalized = this % 1f
    if (normalized < 0f) normalized += 1f
    return normalized
}

internal fun unwrapTrackFraction(fraction: Float, previousFraction: Float?): Float {
    previousFraction ?: return fraction
    var candidate = fraction
    while (candidate < previousFraction - 0.02f) {
        candidate += 1f
    }
    while (
        candidate - 1f >= previousFraction - 0.02f &&
        kotlin.math.abs((candidate - 1f) - previousFraction) < kotlin.math.abs(candidate - previousFraction)
    ) {
        candidate -= 1f
    }
    return candidate
}

internal fun SessionAnalysisFractionPointUi.toTrackMapPoint(): SessionAnalysisTrackMapPoint =
    SessionAnalysisTrackMapPoint(
        x = x,
        y = y,
    )
