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
