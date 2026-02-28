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
    alpha: Float? = null,
): Color = color.copy(alpha = (alpha ?: LocalHudBackgroundOpacity.current).coerceIn(0f, 1f))
