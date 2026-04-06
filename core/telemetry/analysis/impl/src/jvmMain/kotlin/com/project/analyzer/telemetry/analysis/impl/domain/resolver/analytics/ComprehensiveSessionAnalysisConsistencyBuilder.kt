package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.ConsistencyAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.Trend
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisConsistencyReport
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.CornerConsistency as ApiCornerConsistency

/**
 * Folds lap spread and corner repeatability into the comprehensive consistency section.
 */
@Inject
internal class ComprehensiveSessionAnalysisConsistencyBuilder {

    internal fun build(
        report: SessionAnalysisReport,
        consistencyReport: SessionAnalysisConsistencyReport?,
    ): ConsistencyAnalysis {
        val validLaps = report.laps.filter { it.isValid && it.isComplete && !it.isPitLap }
        val lapTimes = validLaps.mapNotNull(SessionAnalysisLap::durationMs).map(Int::toLong)
        val corners = consistencyReport?.corners.orEmpty().associate { consistency ->
            consistency.cornerNumber to ApiCornerConsistency(
                cornerNumber = consistency.cornerNumber,
                avgSpeed = consistency.apexSpeedStdKmh ?: 0f,
                stdDev = consistency.apexSpeedStdKmh ?: 0f,
                consistencyScore = consistency.score,
                trend = Trend.STABLE,
            )
        }
        val sectors = report.samples.groupBy(SessionAnalysisSample::sectorIndex)
            .filterKeys { it >= 0 }
            .mapValues { (sectorIndex, samples) ->
                val lapDurations = samples.groupBy(
                    SessionAnalysisSample::lapNumber,
                ).values.map { it.windowDurationMs() }
                com.project.analyzer.telemetry.analysis.api.model.report.analysis.SectorConsistency(
                    sectorNumber = sectorIndex + 1,
                    avgDelta = lapDurations.averageOrNull()?.roundToLong() ?: 0L,
                    stdDev = lapDurations.standardDeviation()?.roundToLong() ?: 0L,
                    consistencyScore = (100f - ((lapDurations.standardDeviation() ?: 0f) / 3f)).roundToInt().coerceIn(
                        0,
                        100,
                    ),
                    trend = toTrend(lapDurations),
                )
            }
        val score = if (corners.isNotEmpty()) {
            corners.values.map(
                ApiCornerConsistency::consistencyScore,
            ).average().roundToInt()
        } else {
            (100f - ((lapTimes.standardDeviation() ?: 0f) / 15f)).roundToInt().coerceIn(0, 100)
        }
        val fatigueLap = detectFatigueOnsetLap(validLaps)
        return ConsistencyAnalysis(
            lapTimes = lapTimes,
            avgLapTime = lapTimes.averageOrNull()?.roundToLong() ?: 0L,
            stdDev = lapTimes.standardDeviation()?.roundToLong() ?: 0L,
            consistencyScore = score,
            cornerConsistency = corners,
            sectorConsistency = sectors,
            mostConsistentSegment = corners.maxByOrNull { (_, value) -> value.consistencyScore }?.key ?: 0,
            leastConsistentSegment = corners.minByOrNull { (_, value) -> value.consistencyScore }?.key ?: 0,
            fatigueDetected = fatigueLap != null,
            fatigueOnsetLap = fatigueLap,
            recommendations = buildRecommendations(
                score,
                corners.minByOrNull { (_, value) -> value.consistencyScore }?.key,
            ),
        )
    }

    private fun buildRecommendations(score: Int, leastConsistentCorner: Int?): List<String> = buildList {
        leastConsistentCorner?.let { add("Work on repeatability in corner $it before chasing more pace.") }
        if (score < 75) add("Pick clearer braking and turn-in references to reduce lap-to-lap spread.")
    }

    private fun detectFatigueOnsetLap(laps: List<SessionAnalysisLap>): Int? {
        if (laps.size < 4) return null
        val lapTimes = laps.mapNotNull(SessionAnalysisLap::durationMs)
        val midpoint = lapTimes.size / 2
        val firstHalf = lapTimes.take(midpoint).averageOrNull() ?: return null
        val secondHalf = lapTimes.drop(midpoint).averageOrNull() ?: return null
        return if (secondHalf - firstHalf >= FATIGUE_DEGRADATION_THRESHOLD_MS) {
            laps.getOrNull(midpoint)?.lapNumber
        } else {
            null
        }
    }

    private fun toTrend(values: List<Float>): Trend {
        if (values.size < 3) return Trend.STABLE
        val first = values.take(values.size / 2).averageOrNull() ?: return Trend.STABLE
        val second = values.drop(values.size / 2).averageOrNull() ?: return Trend.STABLE
        return when {
            second < first - 12f -> Trend.IMPROVING
            second > first + 12f -> Trend.DEGRADING
            else -> Trend.STABLE
        }
    }

    private companion object {

        // Fatigue is only surfaced when the back half of the run slows by at least a quarter second.
        private const val FATIGUE_DEGRADATION_THRESHOLD_MS: Float = 250f
    }
}
