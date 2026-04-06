package com.analyzer.session.analysis.presentation.builder.lap

import com.analyzer.session.analysis.presentation.builder.track.support.SessionAnalysisTrackPointSampler
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample

/**
 * Picks and normalizes the reference lap used by comparison charts and coaching copy.
 */
internal fun findBestReferenceLap(
    segmentLaps: List<SessionAnalysisLap>,
    segmentSamples: List<SessionAnalysisSample>,
    excludingLapNumber: Int? = null,
): SessionAnalysisLap? {
    val lapSamplesByNumber = segmentSamples
        .asSequence()
        .filter { sample -> sample.lapNumber > 0 }
        .groupBy(SessionAnalysisSample::lapNumber)
        .mapValues { (_, lapSamples) ->
            lapSamples.sortedBy(SessionAnalysisSample::sampleIndexInLap)
        }
    val expectedSectorIndexes = lapSamplesByNumber.values
        .asSequence()
        .flatMap { lapSamples -> lapSamples.asSequence() }
        .map(SessionAnalysisSample::sectorIndex)
        .filter { sectorIndex -> sectorIndex >= 0 }
        .distinct()
        .sorted()
        .toSet()
    val representativePathLengthMeters = segmentLaps
        .asSequence()
        .filter { lap ->
            lap.isValid &&
                lap.isComplete &&
                !lap.isPitLap &&
                (excludingLapNumber == null || lap.lapNumber != excludingLapNumber)
        }
        .mapNotNull { lap ->
            lapSamplesByNumber[lap.lapNumber]
                ?.takeIf { it.size >= MIN_REFERENCE_SAMPLE_COUNT }
                ?.pathLengthMeters()
                ?.takeIf { pathLength -> pathLength.isFinite() && pathLength >= MIN_REFERENCE_PATH_METERS }
        }
        .sorted()
        .toList()
        .let(SessionAnalysisTrackPointSampler()::median)

    val candidates = segmentLaps
        .asSequence()
        .filter { lap ->
            (excludingLapNumber == null || lap.lapNumber != excludingLapNumber) &&
                lap.isReferenceCandidate(
                    lapSamples = lapSamplesByNumber[lap.lapNumber].orEmpty(),
                    expectedSectorIndexes = expectedSectorIndexes,
                    representativePathLengthMeters = representativePathLengthMeters,
                )
        }
        .sortedWith(
            compareBy<SessionAnalysisLap> { lap -> lap.durationMs ?: Int.MAX_VALUE }
                .thenByDescending(SessionAnalysisLap::sampleCount)
                .thenByDescending { lap -> lap.avgSpeedKmh ?: 0f }
                .thenByDescending(SessionAnalysisLap::lapNumber),
        )
        .toList()

    return candidates.firstOrNull()
}

internal fun resolveReferenceLap(
    segmentLaps: List<SessionAnalysisLap>,
    segmentSamples: List<SessionAnalysisSample>,
    selectedReferenceLapNumber: Int?,
    selectedLapNumber: Int?,
): SessionAnalysisLap? {
    val customReference = selectedReferenceLapNumber?.let { lapNumber ->
        segmentLaps.firstOrNull { lap -> lap.lapNumber == lapNumber }
    }
    if (customReference != null) {
        val customSamples = segmentSamples
            .asSequence()
            .filter { sample -> sample.lapNumber == customReference.lapNumber }
            .sortedBy(SessionAnalysisSample::sampleIndexInLap)
            .toList()
        val expectedSectorCount = segmentSamples
            .asSequence()
            .map(SessionAnalysisSample::sectorIndex)
            .filter { sectorIndex -> sectorIndex >= 0 }
            .distinct()
            .sorted()
            .toSet()
        val representativePathLengthMeters = segmentLaps
            .asSequence()
            .filter { lap ->
                lap.isValid &&
                    lap.isComplete &&
                    !lap.isPitLap &&
                    lap.lapNumber != customReference.lapNumber
            }
            .mapNotNull { lap ->
                segmentSamples
                    .asSequence()
                    .filter { sample -> sample.lapNumber == lap.lapNumber }
                    .sortedBy(SessionAnalysisSample::sampleIndexInLap)
                    .toList()
                    .takeIf { it.size >= MIN_REFERENCE_SAMPLE_COUNT }
                    ?.pathLengthMeters()
                    ?.takeIf { pathLength -> pathLength.isFinite() && pathLength >= MIN_REFERENCE_PATH_METERS }
            }
            .sorted()
            .toList()
            .let(SessionAnalysisTrackPointSampler()::median)
        if (
            customReference.isReferenceCandidate(
                lapSamples = customSamples,
                expectedSectorIndexes = expectedSectorCount,
                representativePathLengthMeters = representativePathLengthMeters,
            )
        ) {
            return customReference
        }
    }

    return findBestReferenceLap(
        segmentLaps = segmentLaps,
        segmentSamples = segmentSamples,
        excludingLapNumber = selectedLapNumber,
    )
}

private fun SessionAnalysisLap.isReferenceCandidate(
    lapSamples: List<SessionAnalysisSample>,
    expectedSectorIndexes: Set<Int>,
    representativePathLengthMeters: Float?,
): Boolean {
    val hasRequiredLapState = isValid && isComplete && !isPitLap && durationMs != null
    val hasEnoughSamples = sampleCount >= MIN_REFERENCE_SAMPLE_COUNT && lapSamples.size >= MIN_REFERENCE_SAMPLE_COUNT
    if (!hasRequiredLapState || !hasEnoughSamples) {
        return false
    }

    val observedSectorIndexes = lapSamples
        .asSequence()
        .map(SessionAnalysisSample::sectorIndex)
        .filter { sectorIndex -> sectorIndex >= 0 }
        .distinct()
        .sorted()
        .toSet()
    val lapPathLengthMeters = lapSamples.pathLengthMeters()
    val hasUsablePathLength = lapPathLengthMeters.isFinite() && lapPathLengthMeters >= MIN_REFERENCE_PATH_METERS
    val hasComparableCoverage = representativePathLengthMeters == null ||
        lapPathLengthMeters >= representativePathLengthMeters * MIN_REFERENCE_PATH_RATIO
    val hasTrackPositionCoverage = lapSamples.hasTrackPositionCoverage()
    val hasAllSectors = expectedSectorIndexes.isEmpty() || observedSectorIndexes.containsAll(expectedSectorIndexes)
    val hasReliableSectorCoverage = expectedSectorIndexes.size >= 2 || observedSectorIndexes.size >= 2

    return hasAllSectors && (
        (hasTrackPositionCoverage && hasReliableSectorCoverage) ||
            (hasUsablePathLength && hasComparableCoverage)
        )
}

private fun List<SessionAnalysisSample>.pathLengthMeters(): Float {
    if (size < 2) return 0f

    var total = 0f
    for (index in 1 until size) {
        val previous = this[index - 1]
        val current = this[index]
        val startX = previous.trackX?.takeIf(Float::isFinite) ?: continue
        val startY = previous.trackY?.takeIf(Float::isFinite) ?: continue
        val endX = current.trackX?.takeIf(Float::isFinite) ?: continue
        val endY = current.trackY?.takeIf(Float::isFinite) ?: continue
        total += kotlin.math.hypot(endX - startX, endY - startY)
    }
    return total
}

private fun List<SessionAnalysisSample>.hasTrackPositionCoverage(): Boolean {
    val positions = asSequence()
        .mapNotNull(SessionAnalysisSample::trackPosition)
        .filter(Float::isFinite)
        .map { position -> position.coerceIn(0f, 1f) }
        .toList()
    if (positions.size < 16) return false

    val first = positions.first()
    val last = positions.last()
    val spread = (positions.maxOrNull() ?: first) - (positions.minOrNull() ?: first)
    return first <= 0.12f && last >= 0.88f && spread >= 0.80f
}

private const val MIN_REFERENCE_SAMPLE_COUNT: Int = 128
private const val MIN_REFERENCE_PATH_METERS: Float = 500f
private const val MIN_REFERENCE_PATH_RATIO: Float = 0.72f
