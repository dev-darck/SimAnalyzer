package com.project.analyzer.telemetry.analysis.impl.domain.highlight.mapper

import com.project.analyzer.telemetry.analysis.impl.domain.highlight.SessionAnalysisHighlightDraft
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnostic

/**
 * Rephrases setup diagnostics into highlight drafts so setup issues can sit beside driving issues in the feed.
 */
internal fun SessionAnalysisSetupDiagnostic.toHighlightDraft(): SessionAnalysisHighlightDraft =
    SessionAnalysisHighlightDraft(
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
        score = null,
        affectedLaps = affectedLaps.filter { lapNumber -> lapNumber > 0 },
    )
