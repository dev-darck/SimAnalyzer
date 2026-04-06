package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisCoachInsightUi(
    val title: String,
    val description: String,
    val tone: SessionAnalysisCoachTone = SessionAnalysisCoachTone.Neutral,
)
