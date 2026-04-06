package com.analyzer.session.analysis.presentation.mapper

import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackMapUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackPointUi
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlinx.collections.immutable.toImmutableList

/**
 * Translates analysis track maps into the lighter point model used by rendering and hit testing.
 */
internal fun SessionAnalysisTrackMap.toUi(): SessionAnalysisTrackMapUi = SessionAnalysisTrackMapUi(
    points = points.map(SessionAnalysisTrackMapPoint::toUi).toImmutableList(),
    pitPoints = pitPoints.map(SessionAnalysisTrackMapPoint::toUiWithoutWidth).toImmutableList(),
    idealPoints = idealPoints.map(SessionAnalysisTrackMapPoint::toUiWithoutWidth).toImmutableList(),
    minX = minX,
    minY = minY,
    maxX = maxX,
    maxY = maxY,
)

internal fun SessionAnalysisTrackMapPoint.toUi(): SessionAnalysisTrackPointUi = SessionAnalysisTrackPointUi(
    x = x,
    y = y,
    leftWidthMeters = leftWidthMeters,
    rightWidthMeters = rightWidthMeters,
)

internal fun SessionAnalysisTrackMapPoint.toUiWithoutWidth(): SessionAnalysisTrackPointUi = SessionAnalysisTrackPointUi(
    x = x,
    y = y,
    leftWidthMeters = null,
    rightWidthMeters = null,
)
