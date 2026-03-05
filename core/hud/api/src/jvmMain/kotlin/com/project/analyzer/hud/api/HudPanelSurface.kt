package com.project.analyzer.hud.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

public const val DefaultHudBackgroundOpacity: Float = 0.30f

public val LocalHudBackgroundOpacity: ProvidableCompositionLocal<Float> =
    compositionLocalOf { DefaultHudBackgroundOpacity }

@Composable
public fun hudPanelSurfaceColor(
    color: Color,
    alpha: Float = Float.NaN,
): Color {
    val resolvedAlpha = if (alpha.isNaN()) LocalHudBackgroundOpacity.current else alpha
    return color.copy(alpha = resolvedAlpha.coerceIn(0f, 1f))
}
