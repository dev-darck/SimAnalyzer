package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.mapper.finalizeHighlights
import dev.zacsweers.metro.Inject

/**
 * Runs highlight stages in a deterministic order so broad telemetry cues can seed later
 * corner-specific and tyre-specific refinements.
 */
@Inject
internal class SessionAnalysisHighlightPipeline(
    private val telemetryOverviewStage: SessionAnalysisTelemetryOverviewHighlightStage,
    private val cornerDiagnosticsStage: SessionAnalysisCornerDiagnosticsHighlightStage,
    private val cornerHighlightsStage: SessionAnalysisCornerHighlightsHighlightStage,
    private val tyreWindowStage: SessionAnalysisTyreWindowHighlightStage,
) {

    /**
     * Produces the mutable highlight context used by both the persisted highlight list and the
     * downstream comprehensive analysis.
     */
    internal suspend fun analyze(
        input: SessionAnalysisHighlightPipelineInput,
        options: SessionAnalysisHighlightPipelineOptions = SessionAnalysisHighlightPipelineOptions(),
    ): SessionAnalysisHighlightContext = stages(options).fold(input.toContext()) { context, stage ->
        stage.execute(context)
    }

    internal suspend fun execute(
        input: SessionAnalysisHighlightPipelineInput,
        options: SessionAnalysisHighlightPipelineOptions = SessionAnalysisHighlightPipelineOptions(),
    ): List<SessionAnalysisHighlight> = analyze(input, options).drafts.finalizeHighlights()

    private fun stages(options: SessionAnalysisHighlightPipelineOptions): List<SessionAnalysisHighlightStage> =
        buildList {
            addIfEnabled(telemetryOverviewStage, options)
            addIfEnabled(cornerDiagnosticsStage, options)
            addIfEnabled(cornerHighlightsStage, options)
            addIfEnabled(tyreWindowStage, options)
        }

    private fun MutableList<SessionAnalysisHighlightStage>.addIfEnabled(
        stage: SessionAnalysisHighlightStage,
        options: SessionAnalysisHighlightPipelineOptions,
    ) {
        if (stage.isEnabled(options)) add(stage)
    }
}

private fun SessionAnalysisHighlightPipelineInput.toContext(): SessionAnalysisHighlightContext =
    SessionAnalysisHighlightContext(
        samples = samples,
        bestLapBySegmentId = bestLapBySegmentId,
        tyreProfile = tyreProfile,
        cornerZonesBySegmentId = cornerZonesBySegmentId,
    )
