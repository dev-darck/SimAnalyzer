package com.analyzer.session.analysis.presentation.components.graph.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class SessionAnalysisTooltipRow(val label: String, val value: String, val color: Color)
