package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisGraphState(
    val selectedLapLabel: String = "--",
    val referenceLapLabel: String = "--",
    val comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi> = persistentListOf(),
    val charts: ImmutableList<SessionAnalysisTelemetryChartUi> = persistentListOf(),
)
