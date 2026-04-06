package com.analyzer.session.analysis.presentation.builder.track

import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone

/**
 * Bridges corner scores and diagnostic hints into the marker model used by the track canvas.
 */
private const val cornerMarkerDiagnosticAttachmentFraction: Float = 0.038f
private const val cornerMarkerFallbackSpacingFraction: Float = 0.028f
private const val cornerMarkerTightGroupGapFraction: Float = 0.018f

internal fun SessionAnalysisCornerZone.toCornerScore(
    diagnostics: List<CornerScoreUi>,
    markerTrackPosition: Float = apexTrackPosition,
): CornerScoreUi {
    val representativeDiagnostic = diagnostics
        .minWithOrNull(
            compareBy(CornerScoreUi::score)
                .thenByDescending(CornerScoreUi::timeVsReferenceMs),
        )
    return CornerScoreUi(
        cornerNumber = cornerNumber,
        score = representativeDiagnostic?.score ?: 100,
        trackPosition = apexTrackPosition,
        markerTrackPosition = markerTrackPosition,
        mainIssue = representativeDiagnostic?.mainIssue,
        timeVsReferenceMs = representativeDiagnostic?.timeVsReferenceMs ?: 0,
        recommendation = representativeDiagnostic?.recommendation.orEmpty(),
        source = representativeDiagnostic?.source ?: SessionAnalysisDiagnosisSource.DrivingStyle,
    )
}

internal fun List<SessionAnalysisCornerZone>.resolveMarkerTrackPositions(): List<Float> {
    if (isEmpty()) return emptyList()
    val positions = MutableList(size) { index -> this[index].apexTrackPosition }
    var groupStart = 0
    for (index in 1 until size) {
        val previous = this[index - 1]
        val current = this[index]
        if (current.forwardGapFrom(previous) > cornerMarkerTightGroupGapFraction) {
            assignMarkerTrackPositions(
                positions = positions,
                startIndex = groupStart,
                endIndex = index - 1,
            )
            groupStart = index
        }
    }
    assignMarkerTrackPositions(
        positions = positions,
        startIndex = groupStart,
        endIndex = lastIndex,
    )
    return positions
}

private fun List<SessionAnalysisCornerZone>.assignMarkerTrackPositions(
    positions: MutableList<Float>,
    startIndex: Int,
    endIndex: Int,
) {
    if (startIndex > endIndex) return
    if (startIndex == endIndex) {
        positions[startIndex] = this[startIndex].apexTrackPosition
        return
    }
    val groupSize = endIndex - startIndex + 1
    for (index in startIndex..endIndex) {
        val zone = this[index]
        val positionInGroup = index - startIndex + 1
        val localFraction = positionInGroup.toFloat() / (groupSize + 1).toFloat()
        positions[index] = zone.interpolateTrackPosition(localFraction)
    }
}

private fun SessionAnalysisCornerZone.forwardGapFrom(previous: SessionAnalysisCornerZone): Float = when {
    previous.endTrackPosition <= startTrackPosition -> startTrackPosition - previous.endTrackPosition
    else -> (1f - previous.endTrackPosition) + startTrackPosition
}

private fun SessionAnalysisCornerZone.interpolateTrackPosition(fraction: Float): Float {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return if (startTrackPosition <= endTrackPosition) {
        startTrackPosition + (endTrackPosition - startTrackPosition) * clampedFraction
    } else {
        (startTrackPosition + ((1f - startTrackPosition) + endTrackPosition) * clampedFraction)
            .let(::normalizeTrackPosition)
    }
}

private fun normalizeTrackPosition(trackPosition: Float): Float {
    if (!trackPosition.isFinite()) return 0f
    var normalized = trackPosition % 1f
    if (normalized < 0f) normalized += 1f
    return normalized
}

internal fun SessionAnalysisCornerZone.matchDistance(diagnosticCorner: CornerScoreUi): Float? {
    if (cornerNumber == diagnosticCorner.cornerNumber) {
        return 0f
    }
    val bestDistance = minOf(
        circularFractionDistance(apexTrackPosition, diagnosticCorner.trackPosition),
        distanceToTrackPosition(diagnosticCorner.trackPosition),
    )
    return bestDistance.takeIf { distance -> distance <= cornerMarkerDiagnosticAttachmentFraction }
}

private fun SessionAnalysisCornerZone.distanceToTrackPosition(trackPosition: Float): Float = when {
    containsTrackPosition(trackPosition) -> 0f

    else -> minOf(
        circularFractionDistance(startTrackPosition, trackPosition),
        circularFractionDistance(endTrackPosition, trackPosition),
        circularFractionDistance(apexTrackPosition, trackPosition),
    )
}

private fun SessionAnalysisCornerZone.containsTrackPosition(trackPosition: Float): Boolean =
    if (startTrackPosition <= endTrackPosition) {
        trackPosition in startTrackPosition..endTrackPosition
    } else {
        trackPosition >= startTrackPosition || trackPosition <= endTrackPosition
    }

internal fun List<CornerScoreUi>.collapseNearbyCornerScores(): List<CornerScoreUi> {
    if (size <= 1) return this
    val sortedCorners = sortedBy(CornerScoreUi::trackPosition)
    val groups = mutableListOf(mutableListOf(sortedCorners.first()))
    for (corner in sortedCorners.drop(1)) {
        val currentGroup = groups.last()
        if (
            circularFractionDistance(
                currentGroup.last().trackPosition,
                corner.trackPosition,
            ) <= cornerMarkerFallbackSpacingFraction
        ) {
            currentGroup += corner
        } else {
            groups += mutableListOf(corner)
        }
    }
    if (
        groups.size > 1 &&
        circularFractionDistance(
            groups.last().last().trackPosition,
            groups.first().first().trackPosition,
        ) <= cornerMarkerFallbackSpacingFraction
    ) {
        groups.first().addAll(0, groups.removeAt(groups.lastIndex))
    }
    return groups.map { group ->
        group.minWithOrNull(
            compareBy(CornerScoreUi::score)
                .thenByDescending(CornerScoreUi::timeVsReferenceMs),
        ) ?: group.first()
    }
}

internal fun List<CornerScoreUi>.renumberSequentially(): List<CornerScoreUi> = sortedBy(CornerScoreUi::trackPosition)
    .mapIndexed { index, corner ->
        corner.copy(cornerNumber = index + 1)
    }

private fun circularFractionDistance(first: Float, second: Float): Float {
    val directDistance = kotlin.math.abs(first - second)
    return minOf(directDistance, 1f - directDistance)
}
