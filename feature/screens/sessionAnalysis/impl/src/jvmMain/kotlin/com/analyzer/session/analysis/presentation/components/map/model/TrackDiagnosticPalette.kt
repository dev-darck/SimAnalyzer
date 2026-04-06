package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
internal data class TrackDiagnosticPalette(
    val good: Color,
    val warning: Color,
    val critical: Color,
    val oversteer: Color,
    val lockup: Color,
    val wheelSpin: Color,
    val neutral: Color,
    val leftTurn: Color,
    val rightTurn: Color,
    val straightTurn: Color,
)
