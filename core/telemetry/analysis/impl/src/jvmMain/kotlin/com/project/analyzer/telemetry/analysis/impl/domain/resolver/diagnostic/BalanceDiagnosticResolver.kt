package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey
import com.project.analyzer.utils.ext.averageOrNull
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt

@Inject
internal class BalanceDiagnosticResolver : SetupDiagnosticStage {

    override suspend fun analyze(input: SetupDiagnosticInput): SetupDiagnosticStageResult {
        if (input.corners.isEmpty()) return SetupDiagnosticStageResult()

        val diagnostics = mutableListOf<SessionAnalysisSetupDiagnostic>()
        val cornerInsights = linkedMapOf<SessionAnalysisCornerKey, SessionAnalysisCornerSetupInsight>()

        input.corners
            .groupBy { corner -> SessionAnalysisCornerKey(corner.segmentId, corner.cornerNumber) }
            .forEach { (key, groupedCorners) ->
                val affectedLaps = groupedCorners.map(SessionAnalysisCornerAnalysis::lapNumber).distinct().sorted()
                if (affectedLaps.size < 2) return@forEach

                val minimumRepeats = maxOf(2, (affectedLaps.size * setupRepeatRatioThreshold).roundToInt())
                val understeerHits = groupedCorners.filter { corner -> corner.understeerRatio >= setupBalanceRatioWarn }
                if (understeerHits.size >= minimumRepeats) {
                    val recommendation = setupUndersteerRecommendation
                    diagnostics += SessionAnalysisSetupDiagnostic(
                        category = SessionAnalysisHighlightCategory.SetupUndersteer,
                        severity = SessionAnalysisHighlightSeverity.Warning,
                        title = "Chronic understeer in corner ${key.cornerNumber}",
                        description = "Understeer repeats on ${understeerHits.size}/${affectedLaps.size} analysed laps through the same corner.",
                        recommendation = recommendation,
                        diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                        segmentId = key.segmentId,
                        lapNumber = understeerHits.firstOrNull()?.lapNumber ?: 0,
                        sampleIndexInLap = understeerHits.firstOrNull()?.representativeSample?.sampleIndexInLap ?: 0,
                        trackPosition = understeerHits.map(
                            SessionAnalysisCornerAnalysis::apexTrackPosition,
                        ).averageOrNull(),
                        cornerNumber = key.cornerNumber,
                        deltaMs = understeerHits
                            .map(SessionAnalysisCornerAnalysis::timeLossMs)
                            .averageIntOrNull()
                            ?.roundToInt(),
                        affectedLaps = affectedLaps,
                    )
                    cornerInsights[key] = SessionAnalysisCornerSetupInsight(
                        diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                        recommendation = recommendation,
                    )
                }

                val oversteerHits = groupedCorners.filter { corner -> corner.oversteerRatio >= setupBalanceRatioWarn }
                if (oversteerHits.size >= minimumRepeats) {
                    val recommendation = setupOversteerRecommendation
                    diagnostics += SessionAnalysisSetupDiagnostic(
                        category = SessionAnalysisHighlightCategory.SetupOversteer,
                        severity = SessionAnalysisHighlightSeverity.Warning,
                        title = "Chronic oversteer in corner ${key.cornerNumber}",
                        description = "Oversteer repeats on ${oversteerHits.size}/${affectedLaps.size} analysed laps through the same corner.",
                        recommendation = recommendation,
                        diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                        segmentId = key.segmentId,
                        lapNumber = oversteerHits.firstOrNull()?.lapNumber ?: 0,
                        sampleIndexInLap = oversteerHits.firstOrNull()?.representativeSample?.sampleIndexInLap ?: 0,
                        trackPosition = oversteerHits.map(
                            SessionAnalysisCornerAnalysis::apexTrackPosition,
                        ).averageOrNull(),
                        cornerNumber = key.cornerNumber,
                        deltaMs = oversteerHits
                            .map(SessionAnalysisCornerAnalysis::timeLossMs)
                            .averageIntOrNull()
                            ?.roundToInt(),
                        affectedLaps = affectedLaps,
                    )
                    cornerInsights[key] = SessionAnalysisCornerSetupInsight(
                        diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                        recommendation = recommendation,
                    )
                }
            }

        return SetupDiagnosticStageResult(
            diagnostics = diagnostics,
            cornerInsights = cornerInsights,
        )
    }
}
