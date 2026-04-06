package com.analyzer.session.analysis.presentation.components.layout.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
internal data class SessionAnalysisStudioLayoutMetrics(
    val useThreePaneLayout: Boolean,
    val collapseProgress: Float,
    val heroHeight: Dp,
    val leftPaneWidth: Dp,
    val rightPaneWidth: Dp,
)
