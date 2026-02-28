package com.project.analyzer.theme.typography

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily

@Immutable
public data class SimAnalyzerFontFamilies(
    val sans: FontFamily = FontFamily.SansSerif,
    val mono: FontFamily = FontFamily.Monospace,
)

public val DefaultSimAnalyzerFontFamilies: SimAnalyzerFontFamilies = SimAnalyzerFontFamilies()
