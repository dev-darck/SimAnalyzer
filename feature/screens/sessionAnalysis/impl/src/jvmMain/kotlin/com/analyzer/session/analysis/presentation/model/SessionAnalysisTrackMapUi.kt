package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class SessionAnalysisTrackMapUi(
    val points: ImmutableList<SessionAnalysisTrackPointUi>,
    val pitPoints: ImmutableList<SessionAnalysisTrackPointUi>,
    val idealPoints: ImmutableList<SessionAnalysisTrackPointUi>,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
)
