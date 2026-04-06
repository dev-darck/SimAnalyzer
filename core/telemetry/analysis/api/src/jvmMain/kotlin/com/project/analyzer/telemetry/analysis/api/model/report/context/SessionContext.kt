package com.project.analyzer.telemetry.analysis.api.model.report.context

import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import kotlin.time.Duration

public data class SessionContext(
    val vehicleClass: SessionAnalysisVehicleClass = SessionAnalysisVehicleClass.Unknown,
    val vehicleName: String = "",
    val trackName: String = "",
    val trackLayout: String = "",
    val sessionType: SessionAnalysisSessionType = SessionAnalysisSessionType.Unknown,
    val sessionDuration: Duration = Duration.ZERO,
    val isOnline: Boolean = false,
    val isTimedRace: Boolean = false,
    val hasDRS: Boolean = false,
    val hasERS: Boolean = false,
    val tyreCompound: SessionAnalysisTyreCompound = SessionAnalysisTyreCompound.Unknown,
    val fuelLoad: Float = 0f,
    val weatherConditions: SessionAnalysisWeatherCondition = SessionAnalysisWeatherCondition.Unknown,
)
