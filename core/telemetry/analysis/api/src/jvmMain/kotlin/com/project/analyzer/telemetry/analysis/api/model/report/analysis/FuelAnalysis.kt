package com.project.analyzer.telemetry.analysis.api.model.report.analysis

import com.project.analyzer.telemetry.analysis.api.model.report.common.IssueSeverity

public data class FuelAnalysis(
    val startFuel: Float = 0f,
    val fuelUsed: Float = 0f,
    val fuelPerLap: Float = 0f,
    val fuelPerLapTrend: List<Float> = emptyList(),
    val referenceFuelPerLap: Float = 0f,
    val efficiencyDelta: Float = 0f,
    val weightImpact: Float = 0f,
    val estimatedLapsRemaining: Int = 0,
    val liftingCoastingDetected: Boolean = false,
    val liftingCoastingZones: List<Float> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val severity: IssueSeverity = IssueSeverity.NEUTRAL,
)
