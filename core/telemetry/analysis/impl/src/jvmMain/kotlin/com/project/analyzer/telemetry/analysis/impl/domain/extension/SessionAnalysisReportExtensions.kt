package com.project.analyzer.telemetry.analysis.impl.domain.extension

import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZoneDetector

/**
 * Report-level helpers expose cached lookups so downstream logic does not repeatedly rescan session data.
 */
internal fun SessionAnalysisReport.bestLapBySegmentId(): Map<Long, Int?> = laps.bestLapBySegmentId()

internal fun SessionAnalysisReport.defaultSegmentId(): Long? = segments.lastOrNull()?.segmentId

internal fun SessionAnalysisReport.cornerZonesBySegmentId(
    detector: TrackMapCornerZoneDetector,
): Map<Long, List<TrackMapCornerZone>> = cornerZonesBySegmentId
    .takeIf(Map<Long, List<SessionAnalysisCornerZone>>::isNotEmpty)
    ?.mapValues { (_, zones) -> zones.map(SessionAnalysisCornerZone::toInternal) }
    ?: defaultSegmentId()
        ?.let { segmentId ->
            trackMap
                ?.let(detector::detect)
                ?.takeIf { zones -> zones.isNotEmpty() }
                ?.let { zones -> mapOf(segmentId to zones) }
        }
        .orEmpty()

internal fun Map<Long, List<TrackMapCornerZone>>.toApiCornerZonesBySegmentId():
    Map<Long, List<SessionAnalysisCornerZone>> =
    mapValues { (_, zones) -> zones.map(TrackMapCornerZone::toApi) }

private fun TrackMapCornerZone.toApi(): SessionAnalysisCornerZone = SessionAnalysisCornerZone(
    cornerNumber = cornerNumber,
    startTrackPosition = startTrackPosition,
    endTrackPosition = endTrackPosition,
    apexTrackPosition = apexTrackPosition,
    peakCurvature = peakCurvature,
)

private fun SessionAnalysisCornerZone.toInternal(): TrackMapCornerZone = TrackMapCornerZone(
    cornerNumber = cornerNumber,
    startTrackPosition = startTrackPosition,
    endTrackPosition = endTrackPosition,
    apexTrackPosition = apexTrackPosition,
    peakCurvature = peakCurvature,
)
