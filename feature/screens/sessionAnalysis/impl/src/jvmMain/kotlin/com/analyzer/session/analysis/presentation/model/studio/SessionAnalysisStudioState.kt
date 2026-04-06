package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisStudioState(
    val trackCanvas: SessionAnalysisTrackCanvasState? = null,
    val graph: SessionAnalysisGraphState = SessionAnalysisGraphState(),
    val interaction: SessionAnalysisInteractionState = SessionAnalysisInteractionState(),
    val inspector: SessionAnalysisInspectorState = SessionAnalysisInspectorState(),
)
