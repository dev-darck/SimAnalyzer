package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreTemperatureBand
import com.project.analyzer.telemetry.analysis.impl.domain.extension.coreTempCandidates
import com.project.analyzer.telemetry.analysis.impl.domain.extension.hasTyreBand
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.mapper.toHighlightDraft
import dev.zacsweers.metro.Inject

/**
 * Emits tyre-window highlights when temperature or pressure drift becomes meaningful enough to coach on.
 */
@Inject
internal class SessionAnalysisTyreWindowHighlightStage : SessionAnalysisHighlightStage {

    override val stageKey: String = "tyre-window"

    override fun isEnabled(options: SessionAnalysisHighlightPipelineOptions): Boolean =
        options.includeTyreWindowHighlights

    override suspend fun execute(input: SessionAnalysisHighlightContext): SessionAnalysisHighlightContext {
        val drafts = mutableListOf<SessionAnalysisHighlightDraft>()
        val hotSamples = input.samples.filter { sample ->
            sample.hasTyreBand(SessionAnalysisTyreTemperatureBand.Hot) ||
                sample.hasTyreBand(SessionAnalysisTyreTemperatureBand.Critical)
        }
        hotSamples.maxByOrNull { sample -> sample.coreTempCandidates().filterNotNull().maxOrNull() ?: 0f }
            ?.let { sample ->
                val affectedLaps = hotSamples.map { hotSample -> hotSample.lapNumber }.distinct().sorted()
                drafts += sample.toHighlightDraft(
                    category = SessionAnalysisHighlightCategory.TyreOverheat,
                    severity = if (sample.hasTyreBand(SessionAnalysisTyreTemperatureBand.Critical)) {
                        SessionAnalysisHighlightSeverity.Critical
                    } else {
                        SessionAnalysisHighlightSeverity.Warning
                    },
                    title = "Tyre temperatures are above the window",
                    description = "Tyres run hot on ${affectedLaps.size} lap(s), which will hurt grip and consistency.",
                    diagnosisSource = if (affectedLaps.size >= 2) {
                        SessionAnalysisDiagnosisSource.CarSetup
                    } else {
                        SessionAnalysisDiagnosisSource.Mixed
                    },
                    recommendation = "Lower baseline pressure by around 0.5 psi or reduce prolonged slip on corner entry.",
                    affectedLaps = affectedLaps,
                )
            }

        val coldSamples = input.samples.filter { sample ->
            sample.lapNumber >= 2 && sample.hasTyreBand(SessionAnalysisTyreTemperatureBand.Cold)
        }
        coldSamples.maxByOrNull { sample -> sample.deltaToBestMs ?: Int.MIN_VALUE }?.let { sample ->
            val affectedLaps = coldSamples.map { coldSample -> coldSample.lapNumber }.distinct().sorted()
            drafts += sample.toHighlightDraft(
                category = SessionAnalysisHighlightCategory.TyreCold,
                severity = SessionAnalysisHighlightSeverity.Warning,
                title = "Tyres stay below the working window",
                description = "Tyres remain cold even after the first lap, so the carcass is not fully working.",
                diagnosisSource = if (affectedLaps.size >= 2) {
                    SessionAnalysisDiagnosisSource.CarSetup
                } else {
                    SessionAnalysisDiagnosisSource.Mixed
                },
                recommendation = "Lower pressure slightly or use a stronger warm-up phase before pushing for lap time.",
                affectedLaps = affectedLaps,
                deltaMs = sample.deltaToBestMs,
            )
        }

        return input.appendDrafts(drafts)
    }
}
