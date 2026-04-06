package com.project.analyzer.telemetry.analysis.impl.domain.highlight.mapper

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.SessionAnalysisHighlightDraft
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis

/**
 * Maps corner-analysis findings into draft highlights before wording and ranking are finalized.
 */
internal fun SessionAnalysisCornerAnalysis.toHighlightDraft(
    sample: SessionAnalysisSample,
    category: SessionAnalysisHighlightCategory,
    severity: SessionAnalysisHighlightSeverity,
    title: String,
    description: String,
    diagnosisSource: SessionAnalysisDiagnosisSource,
    recommendation: String,
    deltaMs: Int? = null,
    affectedLaps: List<Int> = listOf(lapNumber),
): SessionAnalysisHighlightDraft = SessionAnalysisHighlightDraft(
    category = category,
    severity = severity,
    segmentId = segmentId,
    lapNumber = lapNumber,
    sampleIndexInLap = sample.sampleIndexInLap,
    trackPosition = sample.trackPosition ?: apexTrackPosition,
    title = title,
    description = description,
    deltaMs = deltaMs,
    diagnosisSource = diagnosisSource,
    recommendation = recommendation,
    cornerNumber = cornerNumber,
    score = score,
    affectedLaps = affectedLaps.filter { lapNumber -> lapNumber > 0 },
)
