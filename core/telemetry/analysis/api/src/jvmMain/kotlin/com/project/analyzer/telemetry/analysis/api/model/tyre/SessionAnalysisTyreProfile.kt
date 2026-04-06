package com.project.analyzer.telemetry.analysis.api.model.tyre

import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass

public data class SessionAnalysisTyreProfile(
    val vehicleClass: SessionAnalysisVehicleClass,
    val compoundFamily: SessionAnalysisTyreCompoundFamily,
    val compoundLabel: String = "",
    val surfaceOptimalMinC: Float,
    val surfaceOptimalMaxC: Float,
    val coreOptimalMinC: Float,
    val coreOptimalMaxC: Float,
    val brakeOptimalMinC: Float,
    val brakeOptimalMaxC: Float,
    val pressureOptimalMinPsi: Float,
    val pressureOptimalMaxPsi: Float,
    val innerOuterSpreadWarnC: Float,
    val innerOuterSpreadCriticalC: Float,
    val estimated: Boolean = false,
)
