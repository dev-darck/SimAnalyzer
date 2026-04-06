package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorBoundaryLocalTurnAngleDeg
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorBoundaryTurnAngleDeg
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorCornerMinimumArcLengthMeters
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorMergeContinuationAngleDeg
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorSplitCompositeMinimumArcLengthMeters
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorSplitPeakMinimumLocalProminenceDeg
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorSplitPeakMinimumSeparationMeters
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorSplitPeakMinimumSupportAngleDeg
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.model.CandidateCornerRange
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Filters and scores curvature candidates before they are promoted into stable corner-zone spans.
 */
internal fun List<Int>.groupByDirectionAndGap(
    gapTolerance: Int,
    directionAt: (Int) -> Int,
): List<CandidateCornerRange> {
    if (isEmpty()) return emptyList()
    val sortedIndices = sorted()
    val groups = mutableListOf<CandidateCornerRange>()
    var currentStart = sortedIndices.first()
    var currentEnd = currentStart
    var currentDirection = directionAt(currentStart)

    sortedIndices.drop(1).forEach { index ->
        val nextDirection = directionAt(index)
        if (
            nextDirection == currentDirection &&
            index - currentEnd <= gapTolerance
        ) {
            currentEnd = index
        } else {
            groups += CandidateCornerRange(
                startIndex = currentStart,
                endIndex = currentEnd,
                directionSign = currentDirection,
            )
            currentStart = index
            currentEnd = index
            currentDirection = nextDirection
        }
    }
    groups += CandidateCornerRange(
        startIndex = currentStart,
        endIndex = currentEnd,
        directionSign = currentDirection,
    )
    return groups
}

internal fun CandidateCornerRange.expandRange(
    pointCount: Int,
    supportTurnDegrees: List<Float>,
    localTurnDegrees: List<Float>,
): CandidateCornerRange {
    var startIndex = startIndex
    var endIndex = endIndex
    var expanded = true

    while (expanded) {
        expanded = false

        val previousIndex = wrapIndex(startIndex - 1, pointCount)
        if (
            previousIndex != endIndex &&
            shouldIncludeBoundaryIndex(
                index = previousIndex,
                directionSign = directionSign,
                supportTurnDegrees = supportTurnDegrees,
                localTurnDegrees = localTurnDegrees,
            )
        ) {
            startIndex = previousIndex
            expanded = true
        }

        val nextIndex = wrapIndex(endIndex + 1, pointCount)
        if (
            nextIndex != startIndex &&
            shouldIncludeBoundaryIndex(
                index = nextIndex,
                directionSign = directionSign,
                supportTurnDegrees = supportTurnDegrees,
                localTurnDegrees = localTurnDegrees,
            )
        ) {
            endIndex = nextIndex
            expanded = true
        }
    }

    return CandidateCornerRange(
        startIndex = startIndex,
        endIndex = endIndex,
        directionSign = directionSign,
    )
}

private fun shouldIncludeBoundaryIndex(
    index: Int,
    directionSign: Int,
    supportTurnDegrees: List<Float>,
    localTurnDegrees: List<Float>,
): Boolean = localTurnDegrees[index] * directionSign >= detectorBoundaryLocalTurnAngleDeg ||
    (
        supportTurnDegrees[index] * directionSign >= detectorBoundaryTurnAngleDeg &&
            localTurnDegrees[index] * directionSign >= detectorBoundaryLocalTurnAngleDeg * 0.55f
        )

internal fun List<CandidateCornerRange>.mergeNearbyRanges(
    pointCount: Int,
    supportTurnDegrees: List<Float>,
    mergeGapTolerance: Int,
    tinyMergeGapTolerance: Int,
): List<CandidateCornerRange> {
    if (size <= 1) return this

    val merged = mutableListOf(first())
    for (candidate in drop(1)) {
        val previous = merged.last()
        val gapSamples = forwardGapSamples(
            endIndex = previous.endIndex,
            nextStartIndex = candidate.startIndex,
            pointCount = pointCount,
        )
        val shouldMerge = previous.directionSign == candidate.directionSign &&
            gapSamples <= mergeGapTolerance &&
            (
                gapSamples <= tinyMergeGapTolerance ||
                    maxGapContinuation(
                        startIndex = previous.endIndex,
                        endIndex = candidate.startIndex,
                        pointCount = pointCount,
                        supportTurnDegrees = supportTurnDegrees,
                        directionSign = previous.directionSign,
                    ) >= detectorMergeContinuationAngleDeg
                )
        if (shouldMerge) {
            merged[merged.lastIndex] = CandidateCornerRange(
                startIndex = previous.startIndex,
                endIndex = candidate.endIndex,
                directionSign = previous.directionSign,
            )
        } else {
            merged += candidate
        }
    }

    if (merged.size <= 1) return merged
    val first = merged.first()
    val last = merged.last()
    val wrapGap = forwardGapSamples(
        endIndex = last.endIndex,
        nextStartIndex = first.startIndex,
        pointCount = pointCount,
    )
    val shouldMergeWrap = first.directionSign == last.directionSign &&
        wrapGap <= mergeGapTolerance &&
        (
            wrapGap <= tinyMergeGapTolerance ||
                maxGapContinuation(
                    startIndex = last.endIndex,
                    endIndex = first.startIndex,
                    pointCount = pointCount,
                    supportTurnDegrees = supportTurnDegrees,
                    directionSign = first.directionSign,
                ) >= detectorMergeContinuationAngleDeg
            )
    return if (shouldMergeWrap) {
        buildList {
            add(
                CandidateCornerRange(
                    startIndex = last.startIndex,
                    endIndex = first.endIndex,
                    directionSign = first.directionSign,
                ),
            )
            addAll(merged.drop(1).dropLast(1))
        }
    } else {
        merged
    }
}

internal fun CandidateCornerRange.splitByProminentPeaks(
    pointCount: Int,
    sampleDistanceStepMeters: Float,
    supportTurnDegrees: List<Float>,
    localTurnDegrees: List<Float>,
): List<CandidateCornerRange> {
    if (!sampleDistanceStepMeters.isFinite() || sampleDistanceStepMeters <= 0f) return listOf(this)
    val orderedIndices = indices(pointCount)
    val arcLengthMeters = (orderedIndices.size - 1) * sampleDistanceStepMeters
    if (orderedIndices.size < 6 || arcLengthMeters < detectorSplitCompositeMinimumArcLengthMeters) {
        return listOf(this)
    }

    val minimumPeakSeparationSamples = max(
        2,
        (detectorSplitPeakMinimumSeparationMeters / sampleDistanceStepMeters).roundToInt(),
    )
    val minimumSegmentSamples = max(
        3,
        (detectorCornerMinimumArcLengthMeters / sampleDistanceStepMeters).roundToInt(),
    )
    val directionalSupport = orderedIndices.map { index -> supportTurnDegrees[index] * directionSign }
    val directionalLocalTurn = orderedIndices.map { index -> localTurnDegrees[index] * directionSign }
    val rawPeakPositions = directionalSupport.indices.filter { position ->
        isLocalPeak(
            position = position,
            directionalValues = directionalSupport,
            minimumPeakValue = detectorSplitPeakMinimumSupportAngleDeg,
        )
    }
    val peakPositions = rawPeakPositions.selectSeparatedPeakPositions(
        directionalValues = directionalSupport,
        minimumPeakSeparationSamples = minimumPeakSeparationSamples,
    )
    if (peakPositions.size < 2) return listOf(this)

    val splitPositions = mutableListOf<Int>()
    var segmentStart = 0
    peakPositions.zipWithNext().forEach { (leftPeak, rightPeak) ->
        if (rightPeak - leftPeak < minimumPeakSeparationSamples) return@forEach
        val valleyPosition = ((leftPeak + 1) until rightPeak)
            .minByOrNull(directionalLocalTurn::get)
            ?: return@forEach
        val leftLocalPeak = directionalLocalTurn.peakAround(
            center = leftPeak,
            radius = minimumPeakSeparationSamples / 2,
        )
        val rightLocalPeak = directionalLocalTurn.peakAround(
            center = rightPeak,
            radius = minimumPeakSeparationSamples / 2,
        )
        val valleyLocalTurn = directionalLocalTurn[valleyPosition]
        val localProminence = minOf(leftLocalPeak, rightLocalPeak) - valleyLocalTurn
        if (localProminence < detectorSplitPeakMinimumLocalProminenceDeg) return@forEach

        if (valleyPosition - segmentStart < minimumSegmentSamples) return@forEach
        if (orderedIndices.lastIndex - valleyPosition < minimumSegmentSamples) return@forEach
        splitPositions += valleyPosition
        segmentStart = valleyPosition + 1
    }
    if (splitPositions.isEmpty()) return listOf(this)

    val splitRanges = mutableListOf<CandidateCornerRange>()
    var startPosition = 0
    splitPositions.forEach { splitPosition ->
        splitRanges += CandidateCornerRange(
            startIndex = orderedIndices[startPosition],
            endIndex = orderedIndices[splitPosition],
            directionSign = directionSign,
        )
        startPosition = splitPosition + 1
    }
    splitRanges += CandidateCornerRange(
        startIndex = orderedIndices[startPosition],
        endIndex = orderedIndices.last(),
        directionSign = directionSign,
    )
    return splitRanges.filter { range ->
        range.indices(pointCount).size >= minimumSegmentSamples
    }.ifEmpty { listOf(this) }
}

private fun maxGapContinuation(
    startIndex: Int,
    endIndex: Int,
    pointCount: Int,
    supportTurnDegrees: List<Float>,
    directionSign: Int,
): Float {
    var maxContinuation = 0f
    var index = wrapIndex(startIndex + 1, pointCount)
    while (index != endIndex) {
        maxContinuation = max(
            maxContinuation,
            supportTurnDegrees[index] * directionSign,
        )
        index = wrapIndex(index + 1, pointCount)
    }
    return maxContinuation
}

internal fun List<Float>.directionSignAt(index: Int): Int = when {
    this[index] > 0f -> 1
    this[index] < 0f -> -1
    else -> 0
}

internal fun forwardGapSamples(endIndex: Int, nextStartIndex: Int, pointCount: Int): Int {
    val rawGap = nextStartIndex - endIndex - 1
    return if (rawGap >= 0) rawGap else rawGap + pointCount
}

internal fun CandidateCornerRange.indices(pointCount: Int): List<Int> {
    val result = mutableListOf<Int>()
    var index = startIndex
    result += index
    while (index != endIndex) {
        index = wrapIndex(index + 1, pointCount)
        result += index
    }
    return result
}

private fun isLocalPeak(position: Int, directionalValues: List<Float>, minimumPeakValue: Float): Boolean {
    val current = directionalValues[position]
    if (current < minimumPeakValue) return false
    val previous = directionalValues[(position - 1).coerceAtLeast(0)]
    val next = directionalValues[(position + 1).coerceAtMost(directionalValues.lastIndex)]
    return current >= previous && current >= next
}

private fun List<Int>.selectSeparatedPeakPositions(
    directionalValues: List<Float>,
    minimumPeakSeparationSamples: Int,
): List<Int> {
    if (size <= 1) return this
    val selected = mutableListOf<Int>()
    sortedByDescending(directionalValues::get).forEach { candidate ->
        if (selected.none { existing -> kotlin.math.abs(existing - candidate) < minimumPeakSeparationSamples }) {
            selected += candidate
        }
    }
    return selected.sorted()
}

private fun List<Float>.peakAround(center: Int, radius: Int): Float {
    val safeRadius = radius.coerceAtLeast(1)
    val start = (center - safeRadius).coerceAtLeast(0)
    val end = (center + safeRadius).coerceAtMost(lastIndex)
    return subList(start, end + 1).maxOrNull() ?: get(center)
}
