package com.analyzer.session.analysis.presentation.components.map.overlay

import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi

internal data class TrackMapDiagnosticOverlayFocus(
    val corner: CornerScoreUi?,
    val issues: List<SessionAnalysisHighlightUi>,
)
