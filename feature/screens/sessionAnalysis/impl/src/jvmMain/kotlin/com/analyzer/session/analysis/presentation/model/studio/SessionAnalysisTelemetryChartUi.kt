package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisTelemetryChartUi(
    val kind: SessionAnalysisTelemetryChartKind,
    val title: String,
    val height: Dp,
    val zeroBaseline: Float? = null,
    val series: ImmutableList<SessionAnalysisTelemetryChartSeriesUi> = persistentListOf(),
)
