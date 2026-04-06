package com.analyzer.session.data.repository

import com.analyzer.session.data.analysis.IndexAnalysis
import com.analyzer.session.data.model.SessionBundleLocation
import com.analyzer.session.data.model.SessionLocation

internal fun shouldSkipPlaceholderLocation(
    bundle: SessionBundleLocation,
    location: SessionLocation,
    analysis: IndexAnalysis?,
): Boolean {
    if (bundle.locations.size <= 1) return false
    return isBoundaryPlaceholder(location, analysis)
}

internal fun isBoundaryPlaceholder(location: SessionLocation, analysis: IndexAnalysis?): Boolean {
    if (location.metadata.frameCount > DETAIL_PLACEHOLDER_MAX_FRAMES) return false
    val laps = analysis?.laps.orEmpty()
    if (laps.isEmpty()) return true
    return laps.none { it.complete } &&
        laps.all { lap -> lap.lap == 1 && !lap.complete && lap.totalTimeMs == null }
}

private const val DETAIL_PLACEHOLDER_MAX_FRAMES: Long = 5L
