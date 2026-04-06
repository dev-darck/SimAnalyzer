package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.impl.domain.pipeline.SessionAnalysisStage

/**
 * Contract for highlight stages that each contribute one focused slice of session feedback.
 */
internal interface SessionAnalysisHighlightStage :
    SessionAnalysisStage<SessionAnalysisHighlightContext, SessionAnalysisHighlightContext> {

    val stageKey: String

    fun isEnabled(options: SessionAnalysisHighlightPipelineOptions): Boolean = true
}
