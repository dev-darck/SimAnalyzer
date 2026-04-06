package com.project.analyzer.telemetry.analysis.impl.domain.highlight

/**
 * Keeps highlight limits and stage toggles in one place so the pipeline can be tuned without rewiring stages.
 */
internal data class SessionAnalysisHighlightPipelineOptions(
    val includeTelemetryOverview: Boolean = true,
    val includeCornerDiagnostics: Boolean = true,
    val includeTyreWindowHighlights: Boolean = true,
)
