package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import dev.zacsweers.metro.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

@Inject
internal class DamperDiagnosticResolver : SetupDiagnosticStage {

    override suspend fun analyze(input: SetupDiagnosticInput): SetupDiagnosticStageResult {
        if (input.samples.size < 12) return SetupDiagnosticStageResult()

        val oscillationEvents = input.samples
            .sortedBy(SessionAnalysisSample::timestampNs)
            .zipWithNext()
            .filter { (current, next) ->
                abs((next.lateralG ?: 0f) - (current.lateralG ?: 0f)) >= setupDamperLatDeltaWarn &&
                    abs((next.yawRateRad ?: 0f) - (current.yawRateRad ?: 0f)) >= setupDamperYawDeltaWarn
            }
        val rate = oscillationEvents.size.toFloat() / (input.samples.size - 1).toFloat()
        if (rate < setupDamperRateWarn) return SetupDiagnosticStageResult()

        val anchor = oscillationEvents.first().second
        return SetupDiagnosticStageResult(
            diagnostics = listOf(
                SessionAnalysisSetupDiagnostic(
                    category = SessionAnalysisHighlightCategory.DamperIssue,
                    severity = SessionAnalysisHighlightSeverity.Warning,
                    title = "Platform oscillation under load",
                    description = "Lateral and yaw response oscillate through ${(rate * 100f).roundToInt()}% of frame transitions.",
                    recommendation = damperRecommendation,
                    diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                    segmentId = anchor.segmentId,
                    lapNumber = anchor.lapNumber,
                    sampleIndexInLap = anchor.sampleIndexInLap,
                    trackPosition = anchor.trackPosition,
                    deltaMs = anchor.deltaToBestMs,
                    affectedLaps = oscillationEvents.map { (_, next) -> next.lapNumber }.distinct().sorted(),
                ),
            ),
        )
    }
}
