package com.project.analyzer.telemetry.analysis.impl.domain.resolver.track

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import com.project.analyzer.telemetry.analysis.impl.domain.extension.cumulativeDistances
import com.project.analyzer.telemetry.analysis.impl.domain.extension.nearestIndexToPoint
import dev.zacsweers.metro.Inject
import kotlin.math.hypot

/**
 * Recovers normalized track position from world coordinates when the source telemetry does not
 * provide enough usable track-position samples.
 */
@Inject
internal class SessionAnalysisTrackPositionResolver {

    /**
     * Projects samples onto the track map with a forward-biased search so per-lap progress remains
     * monotonic even when coordinates jitter around the center line.
     */
    fun enrich(
        samples: List<SessionAnalysisSample>,
        trackMapPoints: List<SessionAnalysisTrackMapPoint>?,
    ): List<SessionAnalysisSample> {
        if (trackMapPoints == null || trackMapPoints.size < 2) {
            return enrichFromLapDistance(samples)
        }
        if (samples.hasSufficientTrackPositions()) return samples

        val cumulativeDistances = trackMapPoints.cumulativeDistances()
        val totalDistance = cumulativeDistances.lastOrNull()?.takeIf { it > 0.001f }
            ?: return enrichFromLapDistance(samples)
        val forwardWindow = maxOf(96, trackMapPoints.size / 18)
        val backwardWindow = maxOf(18, trackMapPoints.size / 160)

        return samples.groupBy(SessionAnalysisSample::lapNumber).flatMap { (_, lapSamples) ->
            val sortedLapSamples = lapSamples.sortedBy(SessionAnalysisSample::sampleIndexInLap)
            var lastMatchedIndex: Int? = null

            sortedLapSamples.map { sample ->
                val existingTrackPosition = sample.trackPosition
                if (existingTrackPosition != null && existingTrackPosition.isFinite() && existingTrackPosition > 0f) {
                    return@map sample
                }

                val sampleX = sample.trackX?.takeIf(Float::isFinite) ?: return@map sample
                val sampleY = sample.trackY?.takeIf(Float::isFinite) ?: return@map sample
                val matchedIndex = when (val previousIndex = lastMatchedIndex) {
                    null -> trackMapPoints.nearestIndexToPoint(sampleX, sampleY)

                    else -> {
                        val windowStart = (previousIndex - backwardWindow).coerceAtLeast(0)
                        val windowEnd = (previousIndex + forwardWindow).coerceAtMost(trackMapPoints.lastIndex)
                        val windowIndex = trackMapPoints.nearestIndexToPoint(
                            x = sampleX,
                            y = sampleY,
                            startIndex = windowStart,
                            endIndex = windowEnd,
                        )
                        windowIndex.coerceAtLeast(previousIndex)
                    }
                }
                lastMatchedIndex = matchedIndex

                val fraction = (cumulativeDistances[matchedIndex] / totalDistance).coerceIn(0f, 1f)
                sample.copy(trackPosition = fraction)
            }
        }
    }

    /**
     * Falls back to travelled distance within each lap when no authored track map is available.
     */
    private fun enrichFromLapDistance(samples: List<SessionAnalysisSample>): List<SessionAnalysisSample> {
        if (samples.hasSufficientTrackPositions()) return samples

        return samples.groupBy(SessionAnalysisSample::lapNumber).flatMap { (_, lapSamples) ->
            val sortedLapSamples = lapSamples.sortedBy(SessionAnalysisSample::sampleIndexInLap)
            val samplesWithCoordinates = sortedLapSamples.filter { sample ->
                sample.trackX?.isFinite() == true && sample.trackY?.isFinite() == true
            }
            if (samplesWithCoordinates.size < 2) return@flatMap sortedLapSamples

            val cumulativeDistances = ArrayList<Float>(samplesWithCoordinates.size)
            cumulativeDistances.add(0f)
            for (index in 1 until samplesWithCoordinates.size) {
                val previous = samplesWithCoordinates[index - 1]
                val current = samplesWithCoordinates[index]
                val segmentLength = hypot(
                    (current.trackX ?: 0f) - (previous.trackX ?: 0f),
                    (current.trackY ?: 0f) - (previous.trackY ?: 0f),
                )
                val previousDistance = cumulativeDistances[index - 1]
                cumulativeDistances.add(
                    if (segmentLength.isFinite()) previousDistance + segmentLength else previousDistance,
                )
            }
            val totalDistance = cumulativeDistances.last().takeIf { it > 0.001f } ?: return@flatMap sortedLapSamples
            val distanceByFrameId = samplesWithCoordinates.zip(cumulativeDistances).associate { (sample, distance) ->
                sample.frameId to distance
            }

            sortedLapSamples.map { sample ->
                val existingTrackPosition = sample.trackPosition
                if (existingTrackPosition != null && existingTrackPosition.isFinite() && existingTrackPosition > 0f) {
                    return@map sample
                }

                val distance = distanceByFrameId[sample.frameId] ?: return@map sample
                sample.copy(trackPosition = (distance / totalDistance).coerceIn(0f, 1f))
            }
        }
    }

    private fun List<SessionAnalysisSample>.hasSufficientTrackPositions(): Boolean = count { sample ->
        val trackPosition = sample.trackPosition
        trackPosition != null && trackPosition.isFinite() && trackPosition > 0f
    } > size / 2
}
