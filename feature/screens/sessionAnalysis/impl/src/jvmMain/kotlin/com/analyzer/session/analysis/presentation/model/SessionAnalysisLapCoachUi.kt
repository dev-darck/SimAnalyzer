package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisLapCoachUi(
    val referenceLapLabel: String,
    val summaryTitle: String,
    val summaryDescription: String,
    val metrics: ImmutableList<SessionAnalysisCoachMetricUi> = persistentListOf(),
    val insights: ImmutableList<SessionAnalysisCoachInsightUi> = persistentListOf(),
)
