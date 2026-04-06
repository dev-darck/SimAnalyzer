package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisTyreAnalyticsUi(
    val avgCoreTempC: Float? = null,
    val peakCoreTempC: Float? = null,
    val peakBrakeTempC: Float? = null,
    val pressureSpreadPsi: Float? = null,
    val hottestTyreLabel: String = "--",
    val peakSlip: Float? = null,
)
