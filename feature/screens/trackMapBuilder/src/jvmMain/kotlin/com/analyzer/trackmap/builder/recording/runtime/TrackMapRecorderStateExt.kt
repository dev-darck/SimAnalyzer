package com.analyzer.trackmap.builder.recording.runtime

import com.analyzer.trackmap.domain.model.TrackMapBuilderState
import com.analyzer.trackmap.domain.model.TrackMapSectorMarker

internal fun TrackMapRecorderState.toTrackMapBuilderState(): TrackMapBuilderState = TrackMapBuilderState(
    recording = recording,
    isSaving = isSaving,
    gameId = gameId,
    gameLabel = gameLabel,
    trackId = trackId,
    trackName = trackName,
    layoutId = layoutId,
    referencePoint = referencePoint,
    points = points,
    leftWidthsMeters = leftWidthsMeters,
    rightWidthsMeters = rightWidthsMeters,
    pointCount = pointCount,
    totalDistanceMeters = totalDistanceMeters,
    bounds = bounds,
    currentPosition = currentPosition,
    currentSpeedKmh = currentSpeedKmh,
    pitPoints = pitPoints,
    pitPointCount = pitPointCount,
    pitEntryPoint = pitEntryPoint,
    pitExitPoint = pitExitPoint,
    sectorCount = sectorCount,
    capturedSectorCount = capturedSectorCount,
    sectorMarkers = sectorMarkers.map { marker ->
        TrackMapSectorMarker(
            index = marker.index,
            position = marker.position,
            sampleCount = marker.sampleCount,
        )
    },
    averageTrackWidthMeters = averageTrackWidthMeters,
    leftCoverageRatio = leftCoverageRatio,
    rightCoverageRatio = rightCoverageRatio,
    guidanceText = guidanceText,
    minSpacingMeters = minSpacingMeters,
    maxSpacingMeters = maxSpacingMeters,
    minAngleDeg = minAngleDeg,
    minSpeedKmh = minSpeedKmh,
    fallbackHalfWidthMeters = fallbackHalfWidthMeters,
    lapIndex = lapIndex,
    lapsRecorded = lapsRecorded,
    isInPitLane = isInPitLane,
    pitOverrideActive = pitOverrideActive,
    message = message,
    lastSavedTrackId = lastSavedTrackId,
    lastSavedAtEpochMs = lastSavedAtEpochMs,
)
