package com.project.analyzer.telemetry.analysis.impl.domain.extension

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample

/**
 * Resolves lap completion safely around wraparound points where raw progress alone becomes ambiguous.
 */
internal fun List<SessionAnalysisSample>.hasTrackPositionLapCompletion(): Boolean {
    if (size < 16) return false

    val usableTrackPositions = asSequence()
        .mapNotNull(SessionAnalysisSample::trackPosition)
        .filter(Float::isFinite)
        .map { position -> position.coerceIn(0f, 1f) }
        .toList()
    if (usableTrackPositions.size < 16) return false

    val first = usableTrackPositions.first()
    val last = usableTrackPositions.last()
    val spread = usableTrackPositions.maxOrNull()?.minus(usableTrackPositions.minOrNull() ?: first) ?: 0f
    val startCovered = first <= 0.12f
    val finishCovered = last >= 0.88f
    return spread >= 0.80f && startCovered && finishCovered
}
