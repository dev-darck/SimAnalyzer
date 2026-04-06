package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

@Immutable
internal data class SessionAnalysisTrackSectorBoundary(val label: String, val start: Offset, val end: Offset)
