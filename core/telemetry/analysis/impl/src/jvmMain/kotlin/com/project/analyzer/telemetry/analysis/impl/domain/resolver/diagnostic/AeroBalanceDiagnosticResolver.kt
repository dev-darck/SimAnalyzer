package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.utils.ext.averageOrNull
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt

@Inject
internal class AeroBalanceDiagnosticResolver : SetupDiagnosticStage {

    override suspend fun analyze(input: SetupDiagnosticInput): SetupDiagnosticStageResult {
        val diagnostics = mutableListOf<SessionAnalysisSetupDiagnostic>()

        val highSpeedUndersteer = input.corners.filter { corner ->
            (corner.entrySpeedKmh ?: corner.exitSpeedKmh ?: 0f) >= setupAeroSpeedThresholdKmh &&
                corner.understeerRatio >= setupBalanceRatioWarn
        }
        if (highSpeedUndersteer.size >= 2) {
            diagnostics += SessionAnalysisSetupDiagnostic(
                category = SessionAnalysisHighlightCategory.AeroBalance,
                severity = SessionAnalysisHighlightSeverity.Warning,
                title = "High-speed front aero push",
                description = "Understeer appears repeatedly above ${setupAeroSpeedThresholdKmh.roundToInt()} km/h.",
                recommendation = aeroFrontBalanceRecommendation,
                diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                segmentId = highSpeedUndersteer.first().segmentId,
                lapNumber = highSpeedUndersteer.first().lapNumber,
                sampleIndexInLap = highSpeedUndersteer.first().representativeSample?.sampleIndexInLap ?: 0,
                trackPosition = highSpeedUndersteer.map(
                    SessionAnalysisCornerAnalysis::apexTrackPosition,
                ).averageOrNull(),
                cornerNumber = highSpeedUndersteer.first().cornerNumber,
                deltaMs = highSpeedUndersteer
                    .map(SessionAnalysisCornerAnalysis::timeLossMs)
                    .averageIntOrNull()
                    ?.roundToInt(),
                affectedLaps = highSpeedUndersteer.map(SessionAnalysisCornerAnalysis::lapNumber).distinct().sorted(),
            )
        }

        val highSpeedOversteer = input.corners.filter { corner ->
            (corner.entrySpeedKmh ?: corner.exitSpeedKmh ?: 0f) >= setupAeroSpeedThresholdKmh &&
                corner.oversteerRatio >= setupBalanceRatioWarn
        }
        if (highSpeedOversteer.size >= 2) {
            diagnostics += SessionAnalysisSetupDiagnostic(
                category = SessionAnalysisHighlightCategory.AeroBalance,
                severity = SessionAnalysisHighlightSeverity.Warning,
                title = "High-speed rear instability",
                description = "Oversteer appears repeatedly above ${setupAeroSpeedThresholdKmh.roundToInt()} km/h.",
                recommendation = aeroRearBalanceRecommendation,
                diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                segmentId = highSpeedOversteer.first().segmentId,
                lapNumber = highSpeedOversteer.first().lapNumber,
                sampleIndexInLap = highSpeedOversteer.first().representativeSample?.sampleIndexInLap ?: 0,
                trackPosition = highSpeedOversteer.map(
                    SessionAnalysisCornerAnalysis::apexTrackPosition,
                ).averageOrNull(),
                cornerNumber = highSpeedOversteer.first().cornerNumber,
                deltaMs = highSpeedOversteer
                    .map(SessionAnalysisCornerAnalysis::timeLossMs)
                    .averageIntOrNull()
                    ?.roundToInt(),
                affectedLaps = highSpeedOversteer.map(SessionAnalysisCornerAnalysis::lapNumber).distinct().sorted(),
            )
        }

        return SetupDiagnosticStageResult(diagnostics = diagnostics)
    }
}
