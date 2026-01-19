package com.project.analyzer.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import com.project.analyzer.theme.colors.DarkExtendedColors
import com.project.analyzer.theme.colors.ExtendedColors
import com.project.analyzer.theme.colors.LightExtendedColors

public val LocalExtendedColors: ProvidableCompositionLocal<ExtendedColors> =
    staticCompositionLocalOf { LightExtendedColors }

public object SimAnalyzerTheme {

    public val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
    public val material: ColorScheme
        @Composable get() = extended.material
    public val horizontalGradient: Brush
        @Composable get() = Brush.horizontalGradient(
            0.0f to extended.gradient0,
            0.2f to extended.gradient20,
            0.4f to extended.gradient40,
            0.6f to extended.gradient60,
            0.8f to extended.gradient80,
            1.0f to extended.gradient100
        )
}

@Composable
public fun SimAnalyzerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit = {}
) {

    val theme by remember(darkTheme) {
        mutableStateOf(value = if (darkTheme) DarkExtendedColors else LightExtendedColors)
    }

    CompositionLocalProvider(
        value = LocalExtendedColors provides theme,
    ) {
        MaterialTheme(
            colorScheme = theme.material,
            content = content
        )
    }
}
