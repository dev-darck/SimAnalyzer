package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisCoachMetricUi(
    val label: String,
    val value: String,
    val tone: SessionAnalysisCoachTone = SessionAnalysisCoachTone.Neutral,
)
