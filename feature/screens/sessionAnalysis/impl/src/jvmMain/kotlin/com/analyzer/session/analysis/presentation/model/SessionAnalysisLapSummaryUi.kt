package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisLapSummaryUi(
    val lapNumber: Int,
    val isValid: Boolean,
    val isPitLap: Boolean,
    val isComplete: Boolean,
    val durationMs: Int? = null,
    val sampleCount: Int,
    val avgSpeedKmh: Float? = null,
    val maxSpeedKmh: Float? = null,
    val deltaToBestMs: Int? = null,
    val maxRpm: Float? = null,
    val endFuelLiters: Float? = null,
    val fuelUsedLiters: Float? = null,
    val peakBrakeTempC: Float? = null,
    val peakCoreTempC: Float? = null,
    val diagnosticScore: Int? = null,
)
