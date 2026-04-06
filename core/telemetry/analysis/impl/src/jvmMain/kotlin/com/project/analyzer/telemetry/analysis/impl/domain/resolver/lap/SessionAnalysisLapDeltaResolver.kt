package com.project.analyzer.telemetry.analysis.impl.domain.resolver.lap

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.extension.bucketAt
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt

private const val DELTA_BUCKET_COUNT: Int = 101

/**
 * Estimates frame deltas to the best lap by comparing samples through coarse track-position buckets
 * so laps with different sample densities can still be aligned.
 */
@Inject
internal class SessionAnalysisLapDeltaResolver {

    /**
     * Resolves a per-frame time delta map against the best lap of each segment.
     */
    fun resolve(samples: List<SessionAnalysisSample>, bestLapBySegmentId: Map<Long, Int?>): Map<Long, Int> {
        if (bestLapBySegmentId.isEmpty()) return emptyMap()

        val result = mutableMapOf<Long, Int>()
        samples.groupBy(SessionAnalysisSample::segmentId).forEach { (segmentId, segmentSamples) ->
            val bestLapNumber = bestLapBySegmentId[segmentId] ?: return@forEach
            val bestLapSamples = segmentSamples
                .filter { sample -> sample.lapNumber == bestLapNumber }
                .sortedBy(SessionAnalysisSample::sampleIndexInLap)
            if (bestLapSamples.size < 2) return@forEach

            val referenceByBucket = buildReferenceBuckets(bestLapSamples)
            if (referenceByBucket.none { it != null }) return@forEach

            segmentSamples.groupBy(SessionAnalysisSample::lapNumber).forEach { (_, lapSamples) ->
                val lapStart = lapSamples.firstOrNull()?.timestampNs ?: return@forEach
                lapSamples.forEach { sample ->
                    val trackPosition = sample.trackPosition ?: return@forEach
                    val reference = referenceByBucket.bucketAt(trackPosition) ?: return@forEach
                    val elapsedMs = ((sample.timestampNs - lapStart) / 1_000_000.0).roundToInt()
                    result[sample.frameId] = elapsedMs - reference
                }
            }
        }
        return result
    }

    /**
     * Captures the fastest elapsed time in each normalized lap bucket and propagates known values to
     * fill sparse regions for interpolation.
     */
    private fun buildReferenceBuckets(bestLapSamples: List<SessionAnalysisSample>): List<Int?> {
        val result = MutableList<Int?>(DELTA_BUCKET_COUNT) { null }
        val lapStartNs = bestLapSamples.first().timestampNs

        bestLapSamples.forEach { sample ->
            val trackPosition = sample.trackPosition ?: return@forEach
            val bucket = ((trackPosition.coerceIn(0f, 1f)) * (DELTA_BUCKET_COUNT - 1)).roundToInt()
            val elapsedMs = ((sample.timestampNs - lapStartNs) / 1_000_000.0).roundToInt()
            val existing = result[bucket]
            if (existing == null || elapsedMs < existing) {
                result[bucket] = elapsedMs
            }
        }

        propagateKnownValues(result.indices, result)
        propagateKnownValues(result.indices.reversed(), result)
        return result
    }

    private fun propagateKnownValues(indices: Iterable<Int>, result: MutableList<Int?>) {
        var lastKnown: Int? = null
        indices.forEach { index ->
            if (result[index] == null) {
                result[index] = lastKnown
            } else {
                lastKnown = result[index]
            }
        }
    }
}
