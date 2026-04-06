package com.analyzer.session.analysis.presentation.builder.diagnostic

import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun highlight(
    id: String,
    category: SessionAnalysisHighlightCategory,
    title: String,
    description: String = title,
    diagnosisSource: SessionAnalysisDiagnosisSource,
    priority: Int,
    deltaMs: Int,
    cornerNumber: Int? = null,
    score: Int? = null,
    recommendation: String = "Adjust the zone and repeat the reference line.",
    affectedLaps: ImmutableList<Int> = persistentListOf(2),
): SessionAnalysisHighlightUi = SessionAnalysisHighlightUi(
    id = id,
    category = category,
    severity = if (priority >= 9) {
        SessionAnalysisHighlightSeverity.Critical
    } else {
        SessionAnalysisHighlightSeverity.Warning
    },
    lapNumber = 2,
    title = title,
    description = description,
    trackPosition = cornerNumber?.let { 0.1f * it },
    deltaMs = deltaMs,
    diagnosisSource = diagnosisSource,
    recommendation = recommendation,
    cornerNumber = cornerNumber,
    score = score,
    priority = priority,
    affectedLaps = affectedLaps,
)

