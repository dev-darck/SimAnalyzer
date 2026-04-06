package com.project.analyzer.telemetry.analysis.impl.domain.resolver.vehicle

import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import dev.zacsweers.metro.Inject

/**
 * Infers a usable vehicle class from sparse car metadata so class-specific thresholds can activate automatically.
 */
@Inject
class SessionAnalysisVehicleClassResolver {

    fun resolve(
        gameId: String,
        carModel: String?,
        carLabel: String?,
        vehicleClassHint: String?,
    ): SessionAnalysisVehicleClass {
        val raw = listOfNotNull(vehicleClassHint, carModel, carLabel, gameId)
            .joinToString(" ")
            .lowercase()

        return when {
            raw.contains("formula 1") || raw.contains("f1") -> SessionAnalysisVehicleClass.F1
            raw.contains("formula") || raw.contains("open wheel") -> SessionAnalysisVehicleClass.Formula
            raw.contains("lmp2") -> SessionAnalysisVehicleClass.LMP2
            raw.contains("lmh") || raw.contains("hypercar") -> SessionAnalysisVehicleClass.LMH
            raw.contains("prototype") || raw.contains("lmp") -> SessionAnalysisVehicleClass.Prototype
            raw.contains("gt3") -> SessionAnalysisVehicleClass.GT3
            raw.contains("gt4") -> SessionAnalysisVehicleClass.GT4
            raw.contains("gt2") -> SessionAnalysisVehicleClass.GT2
            raw.contains("gte") || raw.contains("gtd") -> SessionAnalysisVehicleClass.GTE
            raw.contains("tcr") || raw.contains("touring") -> SessionAnalysisVehicleClass.TCR
            else -> SessionAnalysisVehicleClass.Unknown
        }
    }
}
