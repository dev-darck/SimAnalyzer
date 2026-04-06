package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.utils.ext.averageOrNull
import dev.zacsweers.metro.Inject
import java.util.Locale
import kotlin.math.abs

@Inject
internal class TyrePressureDiagnosticResolver : SetupDiagnosticStage {

    override suspend fun analyze(input: SetupDiagnosticInput): SetupDiagnosticStageResult {
        val frontPressures = input.samples.mapNotNull { sample ->
            listOfNotNull(sample.tyreFl?.pressurePsi, sample.tyreFr?.pressurePsi).averageOrNull()
        }
        val rearPressures = input.samples.mapNotNull { sample ->
            listOfNotNull(sample.tyreRl?.pressurePsi, sample.tyreRr?.pressurePsi).averageOrNull()
        }
        val leftPressures = input.samples.mapNotNull { sample ->
            listOfNotNull(sample.tyreFl?.pressurePsi, sample.tyreRl?.pressurePsi).averageOrNull()
        }
        val rightPressures = input.samples.mapNotNull { sample ->
            listOfNotNull(sample.tyreFr?.pressurePsi, sample.tyreRr?.pressurePsi).averageOrNull()
        }
        if (
            frontPressures.isEmpty() &&
            rearPressures.isEmpty() &&
            leftPressures.isEmpty() &&
            rightPressures.isEmpty()
        ) {
            return SetupDiagnosticStageResult()
        }

        val frontAvg = frontPressures.averageOrNull()
        val rearAvg = rearPressures.averageOrNull()
        val leftAvg = leftPressures.averageOrNull()
        val rightAvg = rightPressures.averageOrNull()
        val axisDiff = if (frontAvg != null && rearAvg != null) abs(frontAvg - rearAvg) else 0f
        val sideDiff = if (leftAvg != null && rightAvg != null) abs(leftAvg - rightAvg) else 0f
        val optimalMid = input.tyreProfile?.let { profile ->
            (profile.pressureOptimalMinPsi + profile.pressureOptimalMaxPsi) * 0.5f
        }
        val windowDeviation = listOfNotNull(frontAvg, rearAvg, leftAvg, rightAvg)
            .map { average -> abs(average - (optimalMid ?: average)) }
            .maxOrNull()
            ?: 0f
        if (
            axisDiff < setupPressureAxisWarnPsi &&
            sideDiff < setupPressureSideWarnPsi &&
            windowDeviation < setupPressureWindowWarnPsi
        ) {
            return SetupDiagnosticStageResult()
        }

        val anchorSample = input.samples.maxByOrNull { sample ->
            maxOf(
                abs(sample.frontPressureAverage() - sample.rearPressureAverage()),
                abs(sample.leftPressureAverage() - sample.rightPressureAverage()),
            )
        }

        val recommendation = when {
            axisDiff >= setupPressureAxisWarnPsi ->
                tyrePressureAxisRecommendation

            sideDiff >= setupPressureSideWarnPsi ->
                tyrePressureSideRecommendation

            optimalMid != null && (frontAvg ?: optimalMid) > optimalMid + setupPressureWindowWarnPsi ->
                tyrePressureHighRecommendation

            optimalMid != null && (frontAvg ?: optimalMid) < optimalMid - setupPressureWindowWarnPsi ->
                tyrePressureLowRecommendation

            else -> tyrePressureFallbackRecommendation
        }
        return SetupDiagnosticStageResult(
            diagnostics = listOf(
                SessionAnalysisSetupDiagnostic(
                    category = SessionAnalysisHighlightCategory.TyrePressureImbalance,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Tyre pressure imbalance",
                    description = buildString {
                        if (axisDiff >= setupPressureAxisWarnPsi) {
                            append("Front/rear pressure split is ")
                            append(String.format(Locale.US, "%.2f", axisDiff))
                            append(" psi. ")
                        }
                        if (sideDiff >= setupPressureSideWarnPsi) {
                            append("Left/right pressure split is ")
                            append(String.format(Locale.US, "%.2f", sideDiff))
                            append(" psi. ")
                        }
                        if (optimalMid != null && windowDeviation >= setupPressureWindowWarnPsi) {
                            append("Current averages sit away from the ")
                            append(String.format(Locale.US, "%.1f", optimalMid))
                            append(" psi target window.")
                        }
                    }.trim(),
                    recommendation = recommendation,
                    diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                    segmentId = anchorSample?.segmentId ?: 0L,
                    lapNumber = anchorSample?.lapNumber ?: 0,
                    sampleIndexInLap = anchorSample?.sampleIndexInLap ?: 0,
                    trackPosition = anchorSample?.trackPosition,
                    deltaMs = anchorSample?.deltaToBestMs,
                    affectedLaps = input.samples.map(SessionAnalysisSample::lapNumber).distinct().sorted(),
                ),
            ),
        )
    }
}

private fun SessionAnalysisSample.frontPressureAverage(): Float =
    listOfNotNull(tyreFl?.pressurePsi, tyreFr?.pressurePsi).averageOrNull() ?: 0f

private fun SessionAnalysisSample.rearPressureAverage(): Float =
    listOfNotNull(tyreRl?.pressurePsi, tyreRr?.pressurePsi).averageOrNull() ?: 0f

private fun SessionAnalysisSample.leftPressureAverage(): Float =
    listOfNotNull(tyreFl?.pressurePsi, tyreRl?.pressurePsi).averageOrNull() ?: 0f

private fun SessionAnalysisSample.rightPressureAverage(): Float =
    listOfNotNull(tyreFr?.pressurePsi, tyreRr?.pressurePsi).averageOrNull() ?: 0f
