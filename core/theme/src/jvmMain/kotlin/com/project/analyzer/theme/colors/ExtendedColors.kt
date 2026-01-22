package com.project.analyzer.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
public data class ExtendedColors(
    val material: ColorScheme,
    
    val shadow: Color,
    val shadowSecondary: Color,

    val errorOutline: Color,

    val highPriorityOutline: Color,
    val highPriorityContainer: Color,
    val onHighPriorityContainer: Color,

    val middlePriorityOutline: Color,
    val middlePriorityContainer: Color,
    val onMiddlePriorityContainer: Color,

    val lowPriorityOutline: Color,
    val lowPriorityContainer: Color,
    val onLowPriorityContainer: Color,

    val gradient0: Color,
    val gradient20: Color,
    val gradient40: Color,
    val gradient60: Color,
    val gradient80: Color,
    val gradient100: Color,

    val purple: Color,
    val pink: Color,
    val lightPink: Color,
    val red: Color,
    val amber: Color,
    val yellow: Color,
    val lightGreen: Color,
    val teal: Color,
    val cyan: Color,
    val orange: Color,

    val surface50: Color,
    val onPrimaryContainer50: Color,
    val onSecondaryContainer50: Color,
)
