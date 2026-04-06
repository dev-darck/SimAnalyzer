package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreTemperatureBand

@Immutable
internal data class SessionAnalysisTyreUi(
    val pressurePsi: Float? = null,
    val coreTempC: Float? = null,
    val innerTempC: Float? = null,
    val middleTempC: Float? = null,
    val outerTempC: Float? = null,
    val avgTempC: Float? = null,
    val brakeTempC: Float? = null,
    val slip: Float? = null,
    val load: Float? = null,
    val tempBand: SessionAnalysisTyreTemperatureBand? = null,
)
