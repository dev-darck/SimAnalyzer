package com.project.analyzer.hud.api

import androidx.compose.ui.unit.IntOffset

public sealed interface HudStoredPosition {

    public data class Normalized(public val xFraction: Float, public val yFraction: Float) : HudStoredPosition

    public data class Absolute(public val offset: IntOffset) : HudStoredPosition
}
