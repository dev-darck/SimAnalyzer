package com.analyzer.session.analysis.presentation.components.inspector.support

import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisSetupAdviceItem
import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi

internal fun DiagnosticIssueUi.toSetupAdviceItem(): SessionAnalysisSetupAdviceItem = SessionAnalysisSetupAdviceItem(
    title = title,
    description = description.trim(),
    recommendation = recommendation.trim(),
    lookAt = setupLookAtLabel(),
    source = source,
    trackPosition = trackPosition,
    system = setupSystem(),
)

internal fun SessionAnalysisHighlightUi.toSetupAdviceItem(): SessionAnalysisSetupAdviceItem =
    SessionAnalysisSetupAdviceItem(
        title = title,
        description = description.trim(),
        recommendation = recommendation.trim(),
        lookAt = category.toSetupLookAtLabel(
            cornerNumber = cornerNumber,
            system = setupSystem(),
        ),
        source = diagnosisSource,
        trackPosition = trackPosition,
        system = setupSystem(),
    )
