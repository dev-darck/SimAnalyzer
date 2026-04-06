package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisComparisonPointUi(
    val fraction: Float,
    val trackPosition: Float,
    val selectedFrameId: Long? = null,
    val selectedElapsedMs: Int? = null,
    val referenceElapsedMs: Int? = null,
    val deltaMs: Int? = null,
    val selectedSpeedKmh: Float? = null,
    val referenceSpeedKmh: Float? = null,
    val selectedThrottle: Float? = null,
    val referenceThrottle: Float? = null,
    val selectedBrake: Float? = null,
    val referenceBrake: Float? = null,
    val selectedSteeringAngleRad: Float? = null,
    val referenceSteeringAngleRad: Float? = null,
    val selectedLateralG: Float? = null,
    val referenceLateralG: Float? = null,
    val selectedYawRateRad: Float? = null,
    val referenceYawRateRad: Float? = null,
    val selectedGear: Int? = null,
    val referenceGear: Int? = null,
    val selectedRpm: Float? = null,
    val referenceRpm: Float? = null,
    val selectedFuelLiters: Float? = null,
    val referenceFuelLiters: Float? = null,
)
