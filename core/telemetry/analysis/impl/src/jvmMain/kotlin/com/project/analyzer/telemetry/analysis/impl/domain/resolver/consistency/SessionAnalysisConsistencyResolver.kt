package com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey
import com.project.analyzer.utils.ext.averageOrNull
import dev.zacsweers.metro.Inject
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val consistencyBrakeStdWarnPct: Float = 0.015f
private const val consistencyThrottleStdWarnPct: Float = 0.014f
private const val consistencyApexSpeedStdWarnKmh: Float = 3.2f
private const val consistencyExitSpeedStdWarnKmh: Float = 4.2f
private const val consistencyLineVarianceWarnMeters: Float = 1.4f

/**
 * Measures how repeatable the driver is corner to corner by scoring brake, throttle, speed, and
 * line variance independently before combining them into one consistency report.
 */
@Inject
class SessionAnalysisConsistencyResolver {

    /**
     * Produces an overall and per-corner consistency report from repeated corner analyses.
     */
    internal fun resolve(corners: List<SessionAnalysisCornerAnalysis>): SessionAnalysisConsistencyReport {
        if (corners.isEmpty()) return SessionAnalysisConsistencyReport()

        val cornerReports = corners
            .groupBy { corner -> SessionAnalysisCornerKey(corner.segmentId, corner.cornerNumber) }
            .mapNotNull { (key, groupedCorners) ->
                buildCornerConsistency(
                    key = key,
                    corners = groupedCorners,
                )
            }
            .sortedWith(
                compareBy<SessionAnalysisCornerConsistency>(SessionAnalysisCornerConsistency::segmentId)
                    .thenBy(SessionAnalysisCornerConsistency::cornerNumber),
            )

        val overallScore = if (cornerReports.isEmpty()) {
            100
        } else {
            cornerReports
                .map(SessionAnalysisCornerConsistency::score)
                .average()
                .roundToInt()
                .coerceIn(0, 100)
        }

        return SessionAnalysisConsistencyReport(
            overallScore = overallScore,
            corners = cornerReports,
        )
    }
}

/**
 * Normalizes the main repeatability signals for one physical corner into a single stability score.
 */
private fun buildCornerConsistency(
    key: SessionAnalysisCornerKey,
    corners: List<SessionAnalysisCornerAnalysis>,
): SessionAnalysisCornerConsistency? {
    val affectedLaps = corners.map(SessionAnalysisCornerAnalysis::lapNumber).distinct().sorted()
    if (affectedLaps.size < 2) return null

    val brakeStd = corners.mapNotNull(SessionAnalysisCornerAnalysis::brakePointTrackPosition).standardDeviation()
    val apexSpeedStd = corners.mapNotNull(SessionAnalysisCornerAnalysis::apexSpeedKmh).standardDeviation()
    val throttleStd = corners.mapNotNull(SessionAnalysisCornerAnalysis::throttlePickupTrackPosition).standardDeviation()
    val exitSpeedStd = corners.mapNotNull(SessionAnalysisCornerAnalysis::exitSpeedKmh).standardDeviation()
    val lineVariance = corners.lineVarianceMeters()
    val trackPosition = corners
        .map(SessionAnalysisCornerAnalysis::apexTrackPosition)
        .averageOrNull()
        ?: return null

    var score = 100
    score -= normalizedPenalty(brakeStd, consistencyBrakeStdWarnPct, maxPenalty = 22)
    score -= normalizedPenalty(throttleStd, consistencyThrottleStdWarnPct, maxPenalty = 18)
    score -= normalizedPenalty(apexSpeedStd, consistencyApexSpeedStdWarnKmh, maxPenalty = 20)
    score -= normalizedPenalty(exitSpeedStd, consistencyExitSpeedStdWarnKmh, maxPenalty = 18)
    score -= normalizedPenalty(lineVariance, consistencyLineVarianceWarnMeters, maxPenalty = 22)

    return SessionAnalysisCornerConsistency(
        segmentId = key.segmentId,
        cornerNumber = key.cornerNumber,
        trackPosition = trackPosition.coerceIn(0f, 1f),
        score = score.coerceIn(0, 100),
        brakePointStdPct = brakeStd,
        apexSpeedStdKmh = apexSpeedStd,
        throttlePickupStdPct = throttleStd,
        exitSpeedStdKmh = exitSpeedStd,
        lineVarianceMeters = lineVariance,
        affectedLaps = affectedLaps,
    )
}

private fun normalizedPenalty(value: Float?, warningThreshold: Float, maxPenalty: Int): Int {
    if (value == null || warningThreshold <= 0f) return 0
    return ((value / warningThreshold).coerceAtMost(2.4f) * (maxPenalty / 2.4f))
        .roundToInt()
        .coerceIn(0, maxPenalty)
}

private fun List<SessionAnalysisCornerAnalysis>.lineVarianceMeters(): Float? {
    val apexPoints = mapNotNull { corner ->
        val sample = corner.representativeSample ?: return@mapNotNull null
        val x = sample.trackX ?: return@mapNotNull null
        val y = sample.trackY ?: return@mapNotNull null
        x to y
    }
    if (apexPoints.size < 2) return null

    val centroidX = apexPoints.map { (x, _) -> x }.average().toFloat()
    val centroidY = apexPoints.map { (_, y) -> y }.average().toFloat()
    val distances = apexPoints.map { (x, y) -> hypot(x - centroidX, y - centroidY) }
    return distances.standardDeviation()
}

private fun List<Float>.standardDeviation(): Float? {
    if (size < 2) return null
    val mean = average()
    val divisor = (size - 1).toDouble()
    val variance = sumOf { value ->
        val delta = value - mean
        delta * delta
    } / divisor
    return sqrt(variance).toFloat()
}
