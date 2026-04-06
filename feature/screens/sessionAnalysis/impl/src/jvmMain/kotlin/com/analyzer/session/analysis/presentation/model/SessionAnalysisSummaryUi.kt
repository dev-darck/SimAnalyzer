package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisSummaryUi(
    val lapDeltaLabel: String,
    val biggestLossLabel: String,
    val biggestLossValueLabel: String,
    val consistencyLabel: String,
    val fuelLabel: String,
    val topSpeedLabel: String,
)
