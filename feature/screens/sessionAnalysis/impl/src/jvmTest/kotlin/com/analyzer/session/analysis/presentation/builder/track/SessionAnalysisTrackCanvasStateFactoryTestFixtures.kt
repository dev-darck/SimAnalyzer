package com.analyzer.session.analysis.presentation.builder.track

import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackMapUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackPointUi
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlinx.collections.immutable.persistentListOf

internal fun sessionTrackMapUi(): SessionAnalysisTrackMapUi = SessionAnalysisTrackMapUi(
    points = persistentListOf(
        SessionAnalysisTrackPointUi(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
        SessionAnalysisTrackPointUi(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
        SessionAnalysisTrackPointUi(x = 100f, y = 40f, leftWidthMeters = 6f, rightWidthMeters = 6f),
    ),
    pitPoints = persistentListOf(),
    idealPoints = persistentListOf(),
    minX = 0f,
    minY = 0f,
    maxX = 100f,
    maxY = 40f,
)

internal fun sessionTrackMap(): SessionAnalysisTrackMap = SessionAnalysisTrackMap(
    points = listOf(
        SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
        SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
        SessionAnalysisTrackMapPoint(x = 100f, y = 40f, leftWidthMeters = 6f, rightWidthMeters = 6f),
    ),
    minX = 0f,
    minY = 0f,
    maxX = 100f,
    maxY = 40f,
)

