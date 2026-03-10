package com.analyzer.trackmap.builder.recording.calibration

import com.analyzer.trackmap.builder.recording.geometry.findClosestIndex
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderState
import com.analyzer.trackmap.domain.model.TrackMapSectorMarker
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource

internal fun buildTrackCalibration(request: TrackMapCalibrationRequest): TrackCalibration? {
    val resolvedSectorCount = request.sectorCount.takeIf { it > 0 } ?: return null
    if (request.sectorMarkers.size < resolvedSectorCount) return null

    val gates = LinkedHashMap<Int, Gate>(resolvedSectorCount)
    for (sectorIndex in 1..resolvedSectorCount) {
        val marker = request.sectorMarkers.firstOrNull { it.index == sectorIndex } ?: return null
        val gate = buildGateAtMarker(
            marker = marker,
            points = request.points,
            leftWidthsMeters = request.leftWidthsMeters,
            rightWidthsMeters = request.rightWidthsMeters,
            fallbackHalfWidthMeters = request.snapshot.fallbackHalfWidthMeters,
        ) ?: return null
        gates[sectorIndex] = gate
    }

    val startFinish = gates[1] ?: return null
    val sectors = mutableListOf<SectorCalibration>()
    for (sectorIndex in 1..resolvedSectorCount) {
        val start = gates[sectorIndex] ?: return null
        val finish = if (sectorIndex == resolvedSectorCount) {
            startFinish
        } else {
            gates[sectorIndex + 1] ?: return null
        }
        sectors += SectorCalibration(
            index = sectorIndex,
            start = start,
            finish = finish,
        )
    }

    return TrackCalibration(
        trackId = request.trackId,
        trackName = request.snapshot.trackName.ifBlank { request.trackId },
        layoutId = request.snapshot.layoutId?.trim()?.takeIf { it.isNotBlank() },
        createdAtEpochMs = System.currentTimeMillis(),
        source = TrackCalibrationSource.USER,
        referencePoint = request.snapshot.referencePoint,
        startFinish = startFinish,
        sectors = sectors,
    )
}

internal data class TrackMapCalibrationRequest(
    val snapshot: TrackMapRecorderState,
    val trackId: String,
    val sectorCount: Int,
    val sectorMarkers: List<TrackMapSectorMarker>,
    val points: List<Vec2>,
    val leftWidthsMeters: List<Float>,
    val rightWidthsMeters: List<Float>,
)

private fun buildGateAtMarker(
    marker: TrackMapSectorMarker,
    points: List<Vec2>,
    leftWidthsMeters: List<Float>,
    rightWidthsMeters: List<Float>,
    fallbackHalfWidthMeters: Float,
): Gate? {
    if (points.size < 2) return null

    val nearestIndex = points.findClosestIndex(marker.position)
    val center = points[nearestIndex]
    val forward = localForward(points, nearestIndex)
    val normal = forward.perpLeft().safeNormalized(Vec2.Right)
    val leftWidth = leftWidthsMeters.getOrElse(nearestIndex) { fallbackHalfWidthMeters }
    val rightWidth = rightWidthsMeters.getOrElse(nearestIndex) { fallbackHalfWidthMeters }
    val halfWidth = ((leftWidth + rightWidth) * 0.5f)
        .takeIf { it.isFinite() && it > 0f }
        ?: fallbackHalfWidthMeters

    return Gate.create(
        center = center,
        forward = forward,
        normal = normal,
        halfWidthMeters = halfWidth,
    )
}

private fun localForward(points: List<Vec2>, index: Int): Vec2 {
    val count = points.size
    if (count < 2) return Vec2.Up
    val prev = points[(index - 1 + count) % count]
    val next = points[(index + 1) % count]
    return (next - prev).safeNormalized(Vec2.Up)
}
