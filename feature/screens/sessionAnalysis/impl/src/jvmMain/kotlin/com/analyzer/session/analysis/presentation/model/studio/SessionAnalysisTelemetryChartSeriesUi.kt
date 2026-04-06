package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisTelemetryChartSeriesUi(
    val label: String,
    val tone: SessionAnalysisTelemetrySeriesTone,
    val dashed: Boolean,
    val coverageStartFraction: Float = 0f,
    val coverageEndFraction: Float = 1f,
    val points: ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(),
)
