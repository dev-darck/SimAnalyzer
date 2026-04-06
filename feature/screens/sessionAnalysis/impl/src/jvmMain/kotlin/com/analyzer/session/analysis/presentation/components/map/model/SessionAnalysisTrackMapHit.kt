package com.analyzer.session.analysis.presentation.components.map.model

import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi

internal data class SessionAnalysisTrackMapHit(
    val fraction: Float,
    val target: SessionAnalysisFractionPointUi,
    val distanceSquared: Float,
)
