package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.computeHeadings
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.computeLocalTurnRadians
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.computeSupportTurnDegrees
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.directionSignAt
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.expandRange
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.gaussianSmooth
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.groupByDirectionAndGap
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.mergeNearbyRanges
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.radiusForMeters
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.resample
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.sanitizeTrackPoints
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.splitByProminentPeaks
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.toClosedPath
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.toDetectedZone
import dev.zacsweers.metro.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Detects corner zones from a center-line polyline by smoothing heading changes, grouping sustained
 * turn signals, and splitting multi-apex bends when the peaks remain well supported.
 */
@Inject
class TrackMapCornerZoneDetector {

    /**
     * Converts the rendered track map into ordered corner zones that can be reused by downstream
     * corner, segment, and coaching analysis.
     */
    internal fun detect(trackMap: SessionAnalysisTrackMap): List<TrackMapCornerZone> {
        val sanitizedPoints = trackMap.points.sanitizeTrackPoints()
        if (sanitizedPoints.size < detectorMinimumPointCount) return emptyList()

        val sourcePath = sanitizedPoints.toClosedPath() ?: return emptyList()
        if (sourcePath.totalDistance < detectorMinimumTrackLengthMeters) return emptyList()

        val resampledPath = sourcePath.resample()
        if (resampledPath.size < detectorMinimumResampleCount) return emptyList()

        val smoothedPath = resampledPath.gaussianSmooth(
            radius = resampledPath.radiusForMeters(detectorSmoothingRadiusMeters),
        )
        val stepMeters = sourcePath.totalDistance / smoothedPath.size.toFloat()
        if (!stepMeters.isFinite() || stepMeters <= 0.05f) return emptyList()

        val headingWindow = smoothedPath.radiusForMeters(detectorHeadingWindowMeters)
        val supportWindow = max(
            headingWindow + 1,
            smoothedPath.radiusForMeters(detectorSupportWindowMeters),
        )
        val headings = smoothedPath.computeHeadings(window = headingWindow)
        val localTurnRadians = headings.computeLocalTurnRadians()
        val localTurnDegrees = localTurnRadians.map { radians -> radians * degreesPerRadian }
        val supportTurnDegrees = headings.computeSupportTurnDegrees(window = supportWindow)
        val gapTolerance = max(1, (detectorGroupingGapMeters / stepMeters).roundToInt())
        val mergeGapTolerance = max(1, (detectorMergeGapMeters / stepMeters).roundToInt())
        val tinyMergeGapTolerance = max(1, (detectorTinyMergeGapMeters / stepMeters).roundToInt())

        val candidateGroups = supportTurnDegrees
            .indices
            .filter { index ->
                abs(supportTurnDegrees[index]) >= detectorCandidateTurnAngleDeg &&
                    abs(localTurnDegrees[index]) >= detectorCandidateLocalTurnAngleDeg
            }
            .groupByDirectionAndGap(
                gapTolerance = gapTolerance,
                directionAt = supportTurnDegrees::directionSignAt,
            )
            .map { range ->
                range.expandRange(
                    pointCount = smoothedPath.size,
                    supportTurnDegrees = supportTurnDegrees,
                    localTurnDegrees = localTurnDegrees,
                )
            }
            .mergeNearbyRanges(
                pointCount = smoothedPath.size,
                supportTurnDegrees = supportTurnDegrees,
                mergeGapTolerance = mergeGapTolerance,
                tinyMergeGapTolerance = tinyMergeGapTolerance,
            )

        if (candidateGroups.isEmpty()) return emptyList()

        val sampleDistances = smoothedPath.map { point -> point.distanceMeters }
        return candidateGroups
            .asSequence()
            .flatMap { candidate ->
                val splitCandidates = candidate.splitByProminentPeaks(
                    pointCount = smoothedPath.size,
                    sampleDistanceStepMeters = stepMeters,
                    supportTurnDegrees = supportTurnDegrees,
                    localTurnDegrees = localTurnDegrees,
                )
                val detectedSplitZones = splitCandidates.mapNotNull { splitCandidate ->
                    splitCandidate.toDetectedZone(
                        pointCount = smoothedPath.size,
                        sampleDistanceStepMeters = stepMeters,
                        sampleDistances = sampleDistances,
                        totalDistanceMeters = sourcePath.totalDistance,
                        supportTurnDegrees = supportTurnDegrees,
                        localTurnRadians = localTurnRadians,
                    )
                }
                if (
                    splitCandidates.size > 1 &&
                    detectedSplitZones.size == splitCandidates.size &&
                    detectedSplitZones.all { zone -> zone.qualifies() }
                ) {
                    detectedSplitZones
                } else {
                    listOfNotNull(
                        candidate.toDetectedZone(
                            pointCount = smoothedPath.size,
                            sampleDistanceStepMeters = stepMeters,
                            sampleDistances = sampleDistances,
                            totalDistanceMeters = sourcePath.totalDistance,
                            supportTurnDegrees = supportTurnDegrees,
                            localTurnRadians = localTurnRadians,
                        ),
                    )
                }
            }
            .distinctBy { zone ->
                listOf(
                    (zone.startTrackPosition * 10_000f).roundToInt(),
                    (zone.endTrackPosition * 10_000f).roundToInt(),
                    (zone.apexTrackPosition * 10_000f).roundToInt(),
                )
            }
            .filter { zone -> zone.qualifies() }
            .sortedBy { zone -> zone.orderingTrackPosition }
            .mapIndexed { index, zone -> zone.toZone(cornerNumber = index + 1) }
            .toList()
    }
}
