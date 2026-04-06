package com.project.analyzer.telemetry.analysis.api.model.report.context

import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerThresholds
import com.project.analyzer.telemetry.analysis.api.model.report.setup.SetupCategory
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass

public data class VehicleClassConfig(
    val vehicleClass: SessionAnalysisVehicleClass = SessionAnalysisVehicleClass.Unknown,
    val cornerThresholds: CornerThresholds = CornerThresholds(),
    val tyreProfile: SessionAnalysisTyreProfile? = null,
    val typicalSpeeds: TypicalSpeeds = TypicalSpeeds(),
    val expectedConsistency: Float = 0f,
    val fuelConsumptionRate: Float = 0f,
    val tyreDegradationRate: Float = 0f,
    val setupParameters: List<SetupCategory> = emptyList(),
)
