package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.degreesPerRadian
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.model.CandidateCornerRange
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.model.DetectedTrackMapCornerZone
import kotlin.math.abs

/**
 * Runs the multi-step zone-detection pass that turns a center line into ordered corner regions.
 */
internal fun CandidateCornerRange.toDetectedZone(
    pointCount: Int,
    sampleDistanceStepMeters: Float,
    sampleDistances: List<Float>,
    totalDistanceMeters: Float,
    supportTurnDegrees: List<Float>,
    localTurnRadians: List<Float>,
): DetectedTrackMapCornerZone? {
    val indices = indices(pointCount)
    if (indices.size < 3) return null

    val arcLengthMeters = (indices.size - 1) * sampleDistanceStepMeters
    if (!arcLengthMeters.isFinite() || arcLengthMeters <= 0f) return null

    val headingDeltaDeg = indices
        .drop(1)
        .sumOf { index -> localTurnRadians[index].toDouble() }
        .toFloat()
        .let(::abs)
        .times(degreesPerRadian)
    if (!headingDeltaDeg.isFinite() || headingDeltaDeg <= 0f) return null

    val apexIndex = indices.maxByOrNull { index ->
        abs(supportTurnDegrees[index])
    } ?: return null
    val wrapsAroundStartFinish = startIndex > endIndex
    val apexTrackPosition = if (wrapsAroundStartFinish) {
        wrapAwareMidpoint(
            startTrackPosition = sampleDistances[startIndex] / totalDistanceMeters,
            endTrackPosition = sampleDistances[endIndex] / totalDistanceMeters,
        )
    } else {
        (sampleDistances[apexIndex] / totalDistanceMeters).normalizeTrackPosition()
    }
    val peakCurvature = indices.maxOfOrNull { index ->
        abs(localTurnRadians[index]) / sampleDistanceStepMeters
    } ?: return null

    return DetectedTrackMapCornerZone(
        startTrackPosition = (sampleDistances[startIndex] / totalDistanceMeters).normalizeTrackPosition(),
        endTrackPosition = (sampleDistances[endIndex] / totalDistanceMeters).normalizeTrackPosition(),
        apexTrackPosition = apexTrackPosition,
        peakCurvature = peakCurvature,
        arcLengthMeters = arcLengthMeters,
        headingDeltaDeg = headingDeltaDeg,
        orderingTrackPosition = if (wrapsAroundStartFinish) {
            wrapAwareMidpoint(
                startTrackPosition = sampleDistances[startIndex] / totalDistanceMeters,
                endTrackPosition = sampleDistances[endIndex] / totalDistanceMeters,
            )
        } else {
            (sampleDistances[apexIndex] / totalDistanceMeters).normalizeTrackPosition()
        },
    )
}
