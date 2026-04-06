package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi

@Immutable
internal data class TrackIssueMarker(val issue: SessionAnalysisHighlightUi, val position: Offset, val accent: Color)
