package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import dev.zacsweers.metro.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@Inject
internal class TyreTemperatureDiagnosticResolver : SetupDiagnosticStage {

    override suspend fun analyze(input: SetupDiagnosticInput): SetupDiagnosticStageResult {
        val tyres = input.samples.flatMap { sample ->
            buildList {
                sample.tyreFl?.let { tyre -> add(ResolvedTyreSample(label = "FL", sample = sample, tyre = tyre)) }
                sample.tyreFr?.let { tyre -> add(ResolvedTyreSample(label = "FR", sample = sample, tyre = tyre)) }
                sample.tyreRl?.let { tyre -> add(ResolvedTyreSample(label = "RL", sample = sample, tyre = tyre)) }
                sample.tyreRr?.let { tyre -> add(ResolvedTyreSample(label = "RR", sample = sample, tyre = tyre)) }
            }
        }
        if (tyres.isEmpty()) return SetupDiagnosticStageResult()

        val largestSpread = tyres.maxByOrNull(
            ResolvedTyreSample::spreadMagnitude,
        ) ?: return SetupDiagnosticStageResult()
        val tyre = largestSpread.tyre
        val sample = largestSpread.sample
        val inner = tyre.innerTempC
        val outer = tyre.outerTempC
        if (inner == null || outer == null) return SetupDiagnosticStageResult()

        val spread = inner - outer
        val warnSpread = input.tyreProfile?.innerOuterSpreadWarnC ?: 8f
        if (abs(spread) < warnSpread) return SetupDiagnosticStageResult()

        val recommendation = if (spread > 0f) tyreInnerShoulderRecommendation else tyreOuterShoulderRecommendation
        return SetupDiagnosticStageResult(
            diagnostics = listOf(
                SessionAnalysisSetupDiagnostic(
                    category = SessionAnalysisHighlightCategory.TyreTempImbalance,
                    severity = if (abs(spread) >= (input.tyreProfile?.innerOuterSpreadCriticalC ?: (warnSpread + 4f))) {
                        SessionAnalysisHighlightSeverity.Critical
                    } else {
                        SessionAnalysisHighlightSeverity.Warning
                    },
                    title = "Tyre temperature imbalance",
                    description = if (spread > 0f) {
                        "${largestSpread.label} inner shoulder runs ${abs(spread).roundToInt()}C hotter than outer."
                    } else {
                        "${largestSpread.label} outer shoulder runs ${abs(spread).roundToInt()}C hotter than inner."
                    },
                    recommendation = recommendation,
                    diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                    segmentId = sample.segmentId,
                    lapNumber = sample.lapNumber,
                    sampleIndexInLap = sample.sampleIndexInLap,
                    trackPosition = sample.trackPosition,
                    deltaMs = sample.deltaToBestMs,
                    affectedLaps = listOf(sample.lapNumber),
                ),
            ),
        )
    }
}
