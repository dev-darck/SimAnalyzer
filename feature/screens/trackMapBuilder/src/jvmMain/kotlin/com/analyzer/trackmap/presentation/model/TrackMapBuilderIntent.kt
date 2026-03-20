package com.analyzer.trackmap.presentation.model

import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint

sealed interface TrackMapBuilderIntent {
    data object Start : TrackMapBuilderIntent
    data object Stop : TrackMapBuilderIntent
    data object Reset : TrackMapBuilderIntent
    data object Save : TrackMapBuilderIntent
    data object MarkPitEntry : TrackMapBuilderIntent
    data object MarkPitExit : TrackMapBuilderIntent
    data class SetReferencePoint(val point: ReferencePoint) : TrackMapBuilderIntent
    data class SetFallbackHalfWidthMeters(val value: Float) : TrackMapBuilderIntent
}
