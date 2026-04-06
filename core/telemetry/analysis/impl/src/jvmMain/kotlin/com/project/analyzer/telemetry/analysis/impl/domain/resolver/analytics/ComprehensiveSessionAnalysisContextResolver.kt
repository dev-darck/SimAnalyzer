package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.report.context.SessionAnalysisSessionType
import com.project.analyzer.telemetry.analysis.api.model.report.context.SessionAnalysisTyreCompound
import com.project.analyzer.telemetry.analysis.api.model.report.context.SessionAnalysisWeatherCondition
import com.project.analyzer.telemetry.analysis.api.model.report.context.SessionContext
import com.project.analyzer.telemetry.analysis.api.model.report.context.VehicleClassConfig
import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerThresholds
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreCompoundFamily
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import dev.zacsweers.metro.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Resolves analytics context and class-specific thresholds from the legacy session report.
 */
@Inject
internal class ComprehensiveSessionAnalysisContextResolver {

    internal fun resolve(report: SessionAnalysisReport): SessionContext {
        val segment = report.segments.lastOrNull()
        val duration = when {
            report.segments.isNotEmpty() -> {
                val start = report.segments.minOf { it.startedAtMs }
                val end = report.segments.mapNotNull { it.endedAtMs }.maxOrNull() ?: start
                (end - start).coerceAtLeast(0L).milliseconds
            }

            report.samples.size >= 2 -> (
                (report.samples.last().timestampNs - report.samples.first().timestampNs) /
                    1_000_000L
                ).milliseconds

            else -> Duration.ZERO
        }
        val vehicleName = segment?.carLabel?.takeIf(String::isNotBlank)
            ?: report.header.carLabel.takeIf(String::isNotBlank)
                .orEmpty()
        val trackName = segment?.trackLabel?.takeIf(String::isNotBlank)
            ?: report.header.trackLabel.takeIf(String::isNotBlank)
                .orEmpty()
        val compound = when (report.tyreProfile?.compoundFamily) {
            SessionAnalysisTyreCompoundFamily.Soft -> SessionAnalysisTyreCompound.Soft
            SessionAnalysisTyreCompoundFamily.Medium -> SessionAnalysisTyreCompound.Medium
            SessionAnalysisTyreCompoundFamily.Hard -> SessionAnalysisTyreCompound.Hard
            SessionAnalysisTyreCompoundFamily.Slick -> SessionAnalysisTyreCompound.Slick
            SessionAnalysisTyreCompoundFamily.Intermediate -> SessionAnalysisTyreCompound.Intermediate
            SessionAnalysisTyreCompoundFamily.Wet -> SessionAnalysisTyreCompound.Wet
            SessionAnalysisTyreCompoundFamily.Unknown, null -> SessionAnalysisTyreCompound.Unknown
        }
        val weather = when (report.tyreProfile?.compoundFamily) {
            SessionAnalysisTyreCompoundFamily.Wet -> SessionAnalysisWeatherCondition.Wet
            SessionAnalysisTyreCompoundFamily.Intermediate -> SessionAnalysisWeatherCondition.Mixed
            SessionAnalysisTyreCompoundFamily.Unknown, null -> SessionAnalysisWeatherCondition.Unknown
            else -> SessionAnalysisWeatherCondition.Dry
        }

        return SessionContext(
            vehicleClass = report.vehicleClass,
            vehicleName = vehicleName,
            trackName = trackName,
            trackLayout = trackName.substringAfterLast(" - ", ""),
            sessionType = segment?.sessionTypeLabel.toAnalyticsSessionType(),
            sessionDuration = duration,
            isOnline = false,
            isTimedRace = segment?.sessionTypeLabel.toAnalyticsSessionType() == SessionAnalysisSessionType.Race,
            hasDRS = report.vehicleClass == SessionAnalysisVehicleClass.F1 || vehicleName.lowercase().contains("drs"),
            hasERS = report.vehicleClass in setOf(SessionAnalysisVehicleClass.F1, SessionAnalysisVehicleClass.LMH),
            tyreCompound = compound,
            fuelLoad = report.samples.firstNotNullOfOrNull { it.fuelLiters } ?: 0f,
            weatherConditions = weather,
        )
    }

    internal fun configFor(
        vehicleClass: SessionAnalysisVehicleClass,
        tyreProfile: SessionAnalysisTyreProfile?,
    ): VehicleClassConfig {
        val thresholds = when (vehicleClass) {
            SessionAnalysisVehicleClass.F1 -> CornerThresholds(180f, 120f, 0.18f..0.42f, 8f, 0.25f, 0.24f, 0.12f, 0.88f)
            SessionAnalysisVehicleClass.GT4 -> CornerThresholds(130f, 85f, 0.15f..0.45f, 5f, 0.35f, 0.32f, 0.17f, 0.78f)
            SessionAnalysisVehicleClass.TCR -> CornerThresholds(125f, 82f, 0.15f..0.45f, 5f, 0.35f, 0.28f, 0.17f, 0.76f)
            else -> CornerThresholds(150f, 100f, 0.15f..0.45f, 6f, 0.3f, 0.3f, 0.15f, 0.8f)
        }
        return VehicleClassConfig(
            vehicleClass = vehicleClass,
            cornerThresholds = thresholds,
            tyreProfile = tyreProfile,
        )
    }

    private fun String?.toAnalyticsSessionType(): SessionAnalysisSessionType = when {
        this.orEmpty().lowercase().contains("qual") -> SessionAnalysisSessionType.Qualifying
        this.orEmpty().lowercase().contains("race") -> SessionAnalysisSessionType.Race
        this.orEmpty().lowercase().contains("prac") -> SessionAnalysisSessionType.Practice
        else -> SessionAnalysisSessionType.Unknown
    }
}
