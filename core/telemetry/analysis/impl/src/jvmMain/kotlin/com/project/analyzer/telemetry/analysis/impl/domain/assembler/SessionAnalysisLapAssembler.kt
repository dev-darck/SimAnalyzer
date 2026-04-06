package com.project.analyzer.telemetry.analysis.impl.domain.assembler

import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.extension.brakeTempCandidates
import com.project.analyzer.telemetry.analysis.impl.domain.extension.coreTempCandidates
import com.project.analyzer.telemetry.analysis.impl.domain.extension.hasFlag
import com.project.analyzer.telemetry.analysis.impl.domain.extension.hasTrackPositionLapCompletion
import com.project.analyzer.telemetry.analysis.impl.domain.extension.maxWheelValue
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndexFlags
import com.project.analyzer.utils.ext.averageOrNull
import dev.zacsweers.metro.Inject
import kotlin.math.hypot
import kotlin.math.max

/**
 * Collapses ordered samples into lap summaries while tolerating sessions where track-position data
 * is incomplete or missing.
 */
@Inject
internal class SessionAnalysisLapAssembler {

    /**
     * Groups samples into per-lap summaries and derives validity, completion, fuel, and thermal
     * aggregates from the raw telemetry.
     */
    fun assemble(
        samples: List<SessionAnalysisSample>,
        sessionTypeBySegmentId: Map<Long, String>,
    ): List<SessionAnalysisLap> = samples
        .groupBy { sample -> sample.segmentId to sample.lapNumber }
        .filterKeys { (_, lapNumber) -> lapNumber > 0 }
        .toSortedMap(compareBy({ it.first }, { it.second }))
        .map { (lapKey, lapSamples) ->
            val (segmentId, lapNumber) = lapKey
            val first = lapSamples.first()
            val last = lapSamples.last()
            val durationMs = if (lapSamples.size >= 2) {
                ((last.timestampNs - first.timestampNs) / 1_000_000L).toInt().takeIf { it > 0 }
            } else {
                null
            }
            val isPitLap = lapSamples.any { sample ->
                sample.hasFlag(TelemetryFrameIndexFlags.IN_PIT) ||
                    sample.hasFlag(TelemetryFrameIndexFlags.IN_PIT_LANE)
            }
            val isInvalid = lapSamples.any { sample ->
                sample.hasFlag(TelemetryFrameIndexFlags.INVALID_LAP)
            }
            val isComplete = lapSamples.hasTrackPositionLapCompletion() || isLapCompleteFromGeometry(lapSamples)
            val deltaToBestMs = lapSamples.lastOrNull()?.deltaToBestMs
            val fuelSamples = lapSamples.mapNotNull(SessionAnalysisSample::fuelLiters)
            val firstFuel = fuelSamples.firstOrNull()
            val lastFuel = fuelSamples.lastOrNull()

            SessionAnalysisLap(
                segmentId = segmentId,
                sessionTypeLabel = sessionTypeBySegmentId[segmentId].orEmpty(),
                lapNumber = lapNumber,
                isValid = !isInvalid,
                isPitLap = isPitLap,
                isComplete = isComplete,
                durationMs = durationMs,
                sampleCount = lapSamples.size,
                avgSpeedKmh = lapSamples.mapNotNull(SessionAnalysisSample::speedKmh).averageOrNull(),
                maxSpeedKmh = lapSamples.mapNotNull(SessionAnalysisSample::speedKmh).maxOrNull(),
                deltaToBestMs = deltaToBestMs,
                peakThrottle = lapSamples.mapNotNull(SessionAnalysisSample::throttle).maxOrNull(),
                peakBrake = lapSamples.mapNotNull(SessionAnalysisSample::brake).maxOrNull(),
                maxRpm = lapSamples.mapNotNull(SessionAnalysisSample::rpm).maxOrNull(),
                endFuelLiters = lastFuel,
                fuelUsedLiters = if (firstFuel != null && lastFuel != null) {
                    (firstFuel - lastFuel).takeIf { it >= 0f }
                } else {
                    null
                },
                peakCoreTempC = lapSamples.maxWheelValue { sample -> sample.coreTempCandidates() },
                peakBrakeTempC = lapSamples.maxWheelValue { sample -> sample.brakeTempCandidates() },
            )
        }

    /**
     * Uses geometric closure as a fallback completion signal when track-position telemetry never
     * reaches a clean start/finish wrap.
     */
    private fun isLapCompleteFromGeometry(samples: List<SessionAnalysisSample>): Boolean {
        val geometry = samples.mapNotNull { sample ->
            val x = sample.trackX?.takeIf(Float::isFinite) ?: return@mapNotNull null
            val y = sample.trackY?.takeIf(Float::isFinite) ?: return@mapNotNull null
            x to y
        }
        if (geometry.size < 16) return false

        val minX = geometry.minOf { it.first }
        val minY = geometry.minOf { it.second }
        val maxX = geometry.maxOf { it.first }
        val maxY = geometry.maxOf { it.second }
        val diagonal = hypot(maxX - minX, maxY - minY)
        if (!diagonal.isFinite() || diagonal <= 1f) return false

        val start = geometry.first()
        val finish = geometry.last()
        val closureDistance = hypot(finish.first - start.first, finish.second - start.second)
        return closureDistance <= max(diagonal * 0.25f, 25f)
    }
}
