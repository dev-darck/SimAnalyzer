package com.analyzer.trackmap.presentation.model

internal sealed interface TrackMapBuilderIntent {
    data object Start : TrackMapBuilderIntent
    data object Stop : TrackMapBuilderIntent
    data object Reset : TrackMapBuilderIntent
    data object Save : TrackMapBuilderIntent
    data object MarkPitEntry : TrackMapBuilderIntent
    data object MarkPitExit : TrackMapBuilderIntent
    data class SetReferencePoint(val point: TrackMapReferencePointUi) : TrackMapBuilderIntent
    data class SetFallbackHalfWidthMeters(val value: Float) : TrackMapBuilderIntent
}
