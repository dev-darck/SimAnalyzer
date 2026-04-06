package com.project.analyzer.telemetry.analysis.impl.domain.assembler

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import com.project.analyzer.telemetry.analysis.impl.domain.extension.hasFlag
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndexFlags
import dev.zacsweers.metro.Inject
import kotlin.math.hypot

/**
 * Builds a compact track map from recorded samples, preferring clean closed laps so later spatial
 * analysis is anchored to stable circuit geometry.
 */
@Inject
internal class SessionAnalysisTrackMapAssembler {

    /**
     * Creates a track map candidate and remembers which lap supplied the geometry so callers can
     * retry with the resolved best lap when needed.
     */
    fun build(
        samples: List<SessionAnalysisSample>,
        preferredSegmentId: Long?,
        preferredBestLapNumber: Int?,
    ): SessionAnalysisTrackMapBuildResult {
        val candidateSamples = selectCandidateSamples(
            samples = samples,
            preferredSegmentId = preferredSegmentId,
            preferredBestLapNumber = preferredBestLapNumber,
        )
        val points = buildTrackPoints(candidateSamples)
        if (points.size < 2) {
            return SessionAnalysisTrackMapBuildResult(
                trackMap = null,
                sourceLapNumber = null,
            )
        }

        val minX = points.minOf(SessionAnalysisTrackMapPoint::x)
        val minY = points.minOf(SessionAnalysisTrackMapPoint::y)
        val maxX = points.maxOf(SessionAnalysisTrackMapPoint::x)
        val maxY = points.maxOf(SessionAnalysisTrackMapPoint::y)
        if (maxX <= minX || maxY <= minY) {
            return SessionAnalysisTrackMapBuildResult(
                trackMap = null,
                sourceLapNumber = null,
            )
        }

        return SessionAnalysisTrackMapBuildResult(
            trackMap = SessionAnalysisTrackMap(
                points = points,
                pitPoints = emptyList(),
                idealPoints = emptyList(),
                minX = minX,
                minY = minY,
                maxX = maxX,
                maxY = maxY,
            ),
            sourceLapNumber = candidateSamples.firstOrNull()?.lapNumber,
        )
    }

    /**
     * Chooses the lap that should seed the track map, favoring an explicitly requested best lap and
     * otherwise selecting the cleanest closed lap available.
     */
    private fun selectCandidateSamples(
        samples: List<SessionAnalysisSample>,
        preferredSegmentId: Long?,
        preferredBestLapNumber: Int?,
    ): List<SessionAnalysisSample> {
        val scopedSamples = samples.filter { sample ->
            preferredSegmentId == null || sample.segmentId == preferredSegmentId
        }
        if (scopedSamples.isEmpty()) return emptyList()

        val lapGroups = scopedSamples
            .filter { sample -> sample.lapNumber > 0 }
            .groupBy(SessionAnalysisSample::lapNumber)
        val lapCandidates = lapGroups
            .mapNotNull { (lapNumber, lapSamples) ->
                lapSamples
                    .sortedBy(SessionAnalysisSample::sampleIndexInLap)
                    .toLapCandidate(lapNumber = lapNumber)
            }

        val preferredLapSamples = preferredBestLapNumber
            ?.let { lapNumber -> lapCandidates.firstOrNull { candidate -> candidate.lapNumber == lapNumber } }
            ?.samples
        if (preferredLapSamples != null) {
            return preferredLapSamples
        }

        return lapCandidates
            .bestTrackMapCandidate()
            ?.samples
            ?: scopedSamples.sortedBy(SessionAnalysisSample::timestampNs)
    }

    /**
     * Drops near-duplicate geometry points to keep the derived center line stable and inexpensive
     * to smooth.
     */
    private fun buildTrackPoints(samples: List<SessionAnalysisSample>): List<SessionAnalysisTrackMapPoint> {
        val result = ArrayList<SessionAnalysisTrackMapPoint>(samples.size)
        samples.forEach { sample ->
            val x = sample.trackX?.takeIf(Float::isFinite) ?: return@forEach
            val y = sample.trackY?.takeIf(Float::isFinite) ?: return@forEach
            val nextPoint = SessionAnalysisTrackMapPoint(x = x, y = y)
            val previousPoint = result.lastOrNull()
            if (previousPoint == null || distanceBetween(previousPoint, nextPoint) >= 0.5f) {
                result += nextPoint
            }
        }
        return result
    }

    private fun hasGeometry(sample: SessionAnalysisSample): Boolean =
        sample.trackX?.isFinite() == true && sample.trackY?.isFinite() == true

    private fun List<SessionAnalysisSample>.toLapCandidate(lapNumber: Int): TrackMapLapCandidate? {
        val geometryCount = count(::hasGeometry)
        if (geometryCount < trackMapMinimumLapGeometrySamples) return null
        return TrackMapLapCandidate(
            lapNumber = lapNumber,
            samples = this,
            geometryCount = geometryCount,
            durationMs = durationMs(),
            isInvalid = any { sample -> sample.hasFlag(TelemetryFrameIndexFlags.INVALID_LAP) },
            isPitLap = any { sample ->
                sample.hasFlag(TelemetryFrameIndexFlags.IN_PIT) ||
                    sample.hasFlag(TelemetryFrameIndexFlags.IN_PIT_LANE)
            },
            isClosedLap = isClosedLapGeometry(),
        )
    }

    private fun List<TrackMapLapCandidate>.bestTrackMapCandidate(): TrackMapLapCandidate? = minWithOrNull(
        compareBy<TrackMapLapCandidate> { candidate -> candidate.qualityBucket }
            .thenBy { candidate -> candidate.durationMs ?: Int.MAX_VALUE }
            .thenByDescending { candidate -> candidate.geometryCount },
    )

    private fun List<SessionAnalysisSample>.durationMs(): Int? {
        if (size < 2) return null
        val duration = ((last().timestampNs - first().timestampNs) / 1_000_000L).toInt()
        return duration.takeIf { value -> value > 0 }
    }

    /**
     * Checks whether the sampled geometry closes back onto itself using a scale-aware threshold that
     * works on both short and long circuits.
     */
    private fun List<SessionAnalysisSample>.isClosedLapGeometry(): Boolean {
        val geometry = mapNotNull { sample ->
            val x = sample.trackX?.takeIf(Float::isFinite) ?: return@mapNotNull null
            val y = sample.trackY?.takeIf(Float::isFinite) ?: return@mapNotNull null
            x to y
        }
        if (geometry.size < trackMapMinimumLapGeometrySamples) return false

        val minX = geometry.minOf { point -> point.first }
        val minY = geometry.minOf { point -> point.second }
        val maxX = geometry.maxOf { point -> point.first }
        val maxY = geometry.maxOf { point -> point.second }
        val diagonal = hypot(maxX - minX, maxY - minY)
        if (!diagonal.isFinite() || diagonal <= 1f) return false

        val start = geometry.first()
        val finish = geometry.last()
        val closureDistance = hypot(finish.first - start.first, finish.second - start.second)
        return closureDistance <= maxOf(diagonal * trackMapLapClosureDiagonalRatio, trackMapLapClosureMinimumMeters)
    }

    private fun distanceBetween(first: SessionAnalysisTrackMapPoint, second: SessionAnalysisTrackMapPoint): Float =
        hypot(second.x - first.x, second.y - first.y)
}

private data class TrackMapLapCandidate(
    val lapNumber: Int,
    val samples: List<SessionAnalysisSample>,
    val geometryCount: Int,
    val durationMs: Int?,
    val isInvalid: Boolean,
    val isPitLap: Boolean,
    val isClosedLap: Boolean,
) {

    val isCleanLap: Boolean
        get() = !isInvalid && !isPitLap

    val isCleanClosedLap: Boolean
        get() = isCleanLap && isClosedLap

    val qualityBucket: Int
        get() = when {
            isCleanClosedLap -> 0
            isClosedLap -> 1
            isCleanLap -> 2
            else -> 3
        }
}

private const val trackMapMinimumLapGeometrySamples: Int = 16
private const val trackMapLapClosureDiagonalRatio: Float = 0.25f
private const val trackMapLapClosureMinimumMeters: Float = 25f
