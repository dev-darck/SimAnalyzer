package com.analyzer.trackmap.presentation

import com.analyzer.trackmap.domain.model.TrackMapBuilderState
import com.analyzer.trackmap.presentation.model.TrackMapBuilderUiState
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi

internal fun TrackMapBuilderState.toTrackMapBuilderUiState(): TrackMapBuilderUiState = TrackMapBuilderUiState(
    recording = recording,
    isSaving = isSaving,
    gameId = gameId,
    gameLabel = gameLabel,
    trackId = trackId,
    trackName = trackName,
    layoutId = layoutId,
    referencePoint = referencePoint,
    pointCount = pointCount,
    totalDistanceMeters = totalDistanceMeters,
    averageTrackWidthMeters = averageTrackWidthMeters,
    leftCoverageRatio = leftCoverageRatio,
    rightCoverageRatio = rightCoverageRatio,
    minSpacingMeters = minSpacingMeters,
    maxSpacingMeters = maxSpacingMeters,
    minAngleDeg = minAngleDeg,
    minSpeedKmh = minSpeedKmh,
    fallbackHalfWidthMeters = fallbackHalfWidthMeters,
    lapIndex = lapIndex,
    lapsRecorded = lapsRecorded,
    sectorCount = sectorCount,
    capturedSectorCount = capturedSectorCount,
    isInPitLane = isInPitLane,
    pitOverrideActive = pitOverrideActive,
    pitEntryPointSet = pitEntryPoint != null,
    pitExitPointSet = pitExitPoint != null,
    pitPointCount = pitPointCount,
    guidanceText = guidanceText,
    message = message,
    lastSavedTrackId = lastSavedTrackId,
    preview = toTrackMapPreviewUi(),
)

fun TrackMapBuilderState.toTrackMapPreviewUi(): TrackMapPreviewUi = TrackMapPreviewUi(
    recording = recording,
    points = points,
    leftWidthsMeters = leftWidthsMeters,
    rightWidthsMeters = rightWidthsMeters,
    pointCount = pointCount,
    totalDistanceMeters = totalDistanceMeters,
    bounds = bounds,
    currentPosition = currentPosition,
    pitPoints = pitPoints,
    pitEntryPoint = pitEntryPoint,
    pitExitPoint = pitExitPoint,
    sectorMarkerPositions = sectorMarkers.map { marker -> marker.position },
    averageTrackWidthMeters = averageTrackWidthMeters,
    guidanceText = guidanceText,
)
