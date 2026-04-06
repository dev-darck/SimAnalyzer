package com.analyzer.session.analysis.presentation.mapper

import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import kotlinx.collections.immutable.toImmutableList

/**
 * Maps domain highlights into the UI model consumed by the navigator, hero, and inspector.
 */
internal fun SessionAnalysisHighlight.toUi(): SessionAnalysisHighlightUi = SessionAnalysisHighlightUi(
    id = id,
    category = category,
    severity = severity,
    lapNumber = lapNumber,
    title = title,
    description = description,
    trackPosition = trackPosition,
    deltaMs = deltaMs,
    diagnosisSource = diagnosisSource,
    recommendation = recommendation,
    cornerNumber = cornerNumber,
    score = score,
    priority = priority,
    affectedLaps = affectedLaps.toImmutableList(),
    relatedHighlightIds = relatedHighlightIds.toImmutableList(),
)
