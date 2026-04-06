package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.utils.ext.averageOrNull
import dev.zacsweers.metro.Inject
import kotlin.math.abs

@Inject
internal class BrakeBiasDiagnosticResolver : SetupDiagnosticStage {

    override suspend fun analyze(input: SetupDiagnosticInput): SetupDiagnosticStageResult {
        val straightLockups = input.samples.filter { sample ->
            (sample.speedKmh ?: 0f) >= 120f &&
                abs(sample.steeringAngleRad ?: 0f) <= 0.05f &&
                sample.hasFrontLockupSignal()
        }
        if (straightLockups.size < 3) return SetupDiagnosticStageResult()

        return SetupDiagnosticStageResult(
            diagnostics = listOf(
                SessionAnalysisSetupDiagnostic(
                    category = SessionAnalysisHighlightCategory.BrakeBalance,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Front axle lockup under straight-line braking",
                    description = "Front wheel slip spikes appear ${straightLockups.size} times while the steering is nearly straight.",
                    recommendation = brakeBiasRecommendation,
                    diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                    segmentId = straightLockups.first().segmentId,
                    lapNumber = straightLockups.first().lapNumber,
                    sampleIndexInLap = straightLockups.first().sampleIndexInLap,
                    trackPosition = straightLockups.mapNotNull { sample -> sample.trackPosition }.averageOrNull(),
                    deltaMs = straightLockups.mapNotNull { sample -> sample.deltaToBestMs }.maxOrNull(),
                    affectedLaps = straightLockups.map { sample -> sample.lapNumber }.distinct().sorted(),
                ),
            ),
        )
    }
}
