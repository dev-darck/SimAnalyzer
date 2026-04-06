package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.trackPositionDistance
import kotlin.math.ceil

/**
 * Window helpers define how corner sample ranges are trimmed and matched before scoring begins.
 */
internal fun List<SessionAnalysisSample>.extractCornerWindows(): List<CornerWindow> {
    if (size < cornerMinWindowSamples) return emptyList()

    val cornerCandidateIndices = indices.filter { index -> this[index].isCornerCandidate() }
    if (cornerCandidateIndices.isEmpty()) return emptyList()

    val grouped = mutableListOf<CornerWindow>()
    var groupStart = cornerCandidateIndices.first()
    var lastIndex = groupStart

    cornerCandidateIndices.drop(1).forEach { index ->
        if (index - lastIndex <= cornerIndexGapTolerance) {
            lastIndex = index
        } else {
            grouped += CornerWindow(
                startIndex = (groupStart - 2).coerceAtLeast(0),
                endIndex = (lastIndex + 2).coerceAtMost(this.lastIndex),
            )
            groupStart = index
            lastIndex = index
        }
    }
    grouped += CornerWindow(
        startIndex = (groupStart - 2).coerceAtLeast(0),
        endIndex = (lastIndex + 2).coerceAtMost(this.lastIndex),
    )

    val merged = mutableListOf<CornerWindow>()
    grouped.sortedBy(CornerWindow::startIndex).forEach { window ->
        val last = merged.lastOrNull()
        when {
            last == null -> merged += window

            window.startIndex <= last.endIndex + cornerIndexGapTolerance -> {
                merged[merged.lastIndex] = last.copy(endIndex = maxOf(last.endIndex, window.endIndex))
            }

            else -> merged += window
        }
    }

    return merged.filter { window -> window.isUsableWindow(this) }
}

internal fun List<SessionAnalysisSample>.extractGeometryWindow(zone: TrackMapCornerZone): CornerWindow? {
    if (size < cornerMinWindowSamples) return null
    if (!zone.wrapsAroundStartFinish) {
        return indices
            .filter { index -> this[index].trackPosition?.let(zone::contains) == true }
            .toCornerWindowOrNull()
            ?.takeIf { window -> window.isUsableWindow(this) }
    }

    val highIndices = indices.filter { index ->
        val trackPosition = this[index].trackPosition ?: return@filter false
        trackPosition >= zone.startTrackPosition
    }
    val lowIndices = indices.filter { index ->
        val trackPosition = this[index].trackPosition ?: return@filter false
        trackPosition <= zone.endTrackPosition
    }
    val candidateWindow = when {
        highIndices.isNotEmpty() && lowIndices.isNotEmpty() -> CornerWindow(
            startIndex = highIndices.first(),
            endIndex = lowIndices.last(),
            wrapsAroundLap = true,
        )

        highIndices.isNotEmpty() -> highIndices.toCornerWindowOrNull()

        lowIndices.isNotEmpty() -> lowIndices.toCornerWindowOrNull()

        else -> null
    }
    return candidateWindow?.takeIf { window -> window.isUsableWindow(this) }
}

internal fun CornerWindow.isUsableWindow(samples: List<SessionAnalysisSample>): Boolean {
    val sampleIndices = sampleIndices(samples.size)
    if (sampleIndices.size < cornerMinWindowSamples) return false
    val startTrackPosition = samples.getOrNull(sampleIndices.first())?.trackPosition ?: return false
    val endTrackPosition = samples.getOrNull(sampleIndices.last())?.trackPosition ?: return false
    val span = if (wrapsAroundLap) {
        (1f - startTrackPosition) + endTrackPosition
    } else {
        endTrackPosition - startTrackPosition
    }
    return span >= cornerMinWindowSpanPct
}

internal fun List<CornerWindow>.matchCornerWindows(
    allSamples: List<SessionAnalysisSample>,
    referenceCorners: List<SessionAnalysisCornerReference>,
): List<MatchedCornerWindow> {
    if (isEmpty()) return emptyList()
    if (referenceCorners.isEmpty()) {
        return sortedBy { window ->
            allSamples.apexTrackPositionFor(window) ?: 0f
        }.mapIndexed { index, window ->
            MatchedCornerWindow(
                cornerNumber = index + 1,
                window = window,
            )
        }
    }

    val unmatchedReferences = referenceCorners.toMutableList()
    var syntheticCornerNumber = (referenceCorners.maxOfOrNull(SessionAnalysisCornerReference::cornerNumber) ?: 0) + 1

    return sortedBy { window ->
        allSamples.apexTrackPositionFor(window) ?: 0f
    }.map { window ->
        val apexTrackPosition = allSamples.apexTrackPositionFor(window)
        val exactMatch = apexTrackPosition?.let { apex ->
            unmatchedReferences.minByOrNull { reference ->
                trackPositionDistance(reference.apexTrackPosition, apex)
            }?.takeIf { reference ->
                trackPositionDistance(reference.apexTrackPosition, apex) <= cornerReferenceMatchWindowPct
            }
        }
        if (exactMatch != null) {
            unmatchedReferences.remove(exactMatch)
            MatchedCornerWindow(
                cornerNumber = exactMatch.cornerNumber,
                window = window,
                reference = exactMatch,
            )
        } else {
            val relaxedMatch = apexTrackPosition?.let { apex ->
                referenceCorners.minByOrNull { reference ->
                    trackPositionDistance(reference.apexTrackPosition, apex)
                }?.takeIf { reference ->
                    trackPositionDistance(reference.apexTrackPosition, apex) <= cornerReferenceMatchWindowPct * 1.7f
                }
            }
            MatchedCornerWindow(
                cornerNumber = relaxedMatch?.cornerNumber ?: syntheticCornerNumber++,
                window = window,
                reference = relaxedMatch,
            )
        }
    }
}

internal fun List<SessionAnalysisSample>.apexTrackPositionFor(window: CornerWindow): Float? {
    val indices = window.sampleIndices(size)
    if (indices.isEmpty()) return null
    return indices
        .map(this::get)
        .minByOrNull { sample -> sample.speedKmh ?: Float.MAX_VALUE }
        ?.trackPosition
}

internal fun minimumGeometryCoverage(cornerCount: Int): Int =
    ceil(cornerCount.coerceAtLeast(1) * geometryCornerCoverageRatio).toInt()

private fun List<Int>.toCornerWindowOrNull(): CornerWindow? = if (isEmpty()) {
    null
} else {
    CornerWindow(
        startIndex = first(),
        endIndex = last(),
    )
}
