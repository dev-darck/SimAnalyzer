package com.analyzer.trackmap.builder.recording.save

import com.analyzer.trackmap.builder.recording.geometry.findClosestIndex
import com.analyzer.trackmap.data.library.TrackMapStatsCalculator
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint

internal data class TrackMapSavePayload(
    val gameId: String,
    val trackId: String,
    val trackName: String,
    val layoutId: String,
    val referencePoint: ReferencePoint,
    val points: List<Vec2>,
    val leftWidthsMeters: List<Float>,
    val rightWidthsMeters: List<Float>,
    val pitPoints: List<Vec2>,
    val bounds: TrackMapBounds?,
    val pitEntryPoint: Vec2?,
    val pitExitPoint: Vec2?,
)

internal fun TrackMapSavePayload.toTrackMap(stats: TrackMapStatsCalculator): TrackMap {
    val resolvedBounds = bounds ?: stats.computeBounds(points) ?: TrackMapBounds(0f, 0f, 0f, 0f)
    val pitEntryIndex = pitEntryPoint?.let(points::findClosestIndex) ?: -1
    val pitExitIndex = pitExitPoint?.let(points::findClosestIndex) ?: -1

    return TrackMap(
        gameId = gameId,
        trackId = trackId,
        trackName = trackName,
        layoutId = layoutId,
        createdAtEpochMs = System.currentTimeMillis(),
        referencePoint = referencePoint,
        points = points.mapIndexed { index, point ->
            TrackMapPoint.from(
                v = point,
                leftWidthMeters = leftWidthsMeters.getOrElse(index) { 0f },
                rightWidthMeters = rightWidthsMeters.getOrElse(index) { 0f },
            )
        },
        pitPoints = pitPoints.map { TrackMapPoint.from(it) },
        bounds = resolvedBounds,
        pitEntryIndex = pitEntryIndex,
        pitExitIndex = pitExitIndex,
    )
}
