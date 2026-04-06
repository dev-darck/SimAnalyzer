package com.project.analyzer.telemetry.analysis.impl.domain.highlight.mapper

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.SessionAnalysisHighlightDraft

/**
 * Extracts short-lived telemetry events from raw samples and turns them into highlight candidates.
 */
internal fun SessionAnalysisSample.toHighlightDraft(
    category: SessionAnalysisHighlightCategory,
    severity: SessionAnalysisHighlightSeverity,
    title: String,
    description: String,
    deltaMs: Int? = null,
    diagnosisSource: SessionAnalysisDiagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
    recommendation: String = "",
    cornerNumber: Int? = null,
    score: Int? = null,
    affectedLaps: List<Int> = listOf(lapNumber).filter { lapNumber -> lapNumber > 0 },
): SessionAnalysisHighlightDraft = SessionAnalysisHighlightDraft(
    category = category,
    severity = severity,
    segmentId = segmentId,
    lapNumber = lapNumber,
    sampleIndexInLap = sampleIndexInLap,
    trackPosition = trackPosition,
    title = title,
    description = description,
    deltaMs = deltaMs,
    diagnosisSource = diagnosisSource,
    recommendation = recommendation,
    cornerNumber = cornerNumber,
    score = score,
    affectedLaps = affectedLaps,
)
