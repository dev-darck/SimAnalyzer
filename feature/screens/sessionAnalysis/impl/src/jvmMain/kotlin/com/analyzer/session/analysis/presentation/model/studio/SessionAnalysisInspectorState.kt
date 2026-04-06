package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisInspectorState(
    val selectedTyreAnalytics: SessionAnalysisTyreAnalyticsUi = SessionAnalysisTyreAnalyticsUi(),
    val referenceTyreAnalytics: SessionAnalysisTyreAnalyticsUi = SessionAnalysisTyreAnalyticsUi(),
)
