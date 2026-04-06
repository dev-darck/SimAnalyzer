package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisHeaderUi(
    val sessionTypeLabel: String,
    val carLabel: String,
    val trackLabel: String,
    val startedAtLabel: String,
    val vehicleClassLabel: String,
    val compoundLabel: String,
    val airTempLabel: String,
    val trackTempLabel: String,
    val bestLapLabel: String,
    val selectedLapLabel: String,
    val lapTimeLabel: String,
    val validLapsLabel: String,
    val topSpeedLabel: String,
    val peakBrakeLabel: String,
    val peakCoreLabel: String,
    val surfaceWindowLabel: String,
    val coreWindowLabel: String,
    val brakeWindowLabel: String,
)
