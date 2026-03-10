package com.project.analyzer.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.colors.DarkExtendedColors
import com.project.analyzer.theme.colors.ExtendedColors
import com.project.analyzer.theme.colors.LightExtendedColors
import com.project.analyzer.theme.typography.DefaultSimAnalyzerFontFamilies
import com.project.analyzer.theme.typography.SimAnalyzerFontFamilies
import com.project.analyzer.theme.typography.simAnalyzerTypography

public val LocalExtendedColors: ProvidableCompositionLocal<ExtendedColors> =
    staticCompositionLocalOf { LightExtendedColors }

public val LocalDarkTheme: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { false }

@Immutable
public data class SimAnalyzerCornerTokens(
    val indicator: RoundedCornerShape = RoundedCornerShape(4.dp),
    val compact: RoundedCornerShape = RoundedCornerShape(6.dp),
    val control: RoundedCornerShape = RoundedCornerShape(8.dp),
    val item: RoundedCornerShape = RoundedCornerShape(10.dp),
    val field: RoundedCornerShape = RoundedCornerShape(12.dp),
    val overlay: RoundedCornerShape = RoundedCornerShape(14.dp),
    val card: RoundedCornerShape = RoundedCornerShape(16.dp),
    val badge: RoundedCornerShape = RoundedCornerShape(18.dp),
    val panel: RoundedCornerShape = RoundedCornerShape(20.dp),
    val display: RoundedCornerShape = RoundedCornerShape(26.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(999.dp),
)

@Immutable
public data class SimAnalyzerChromeTokens(
    val dividerSubtle: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val borderEmphasis: Color,
    val borderInteractive: Color,
    val borderInteractiveStrong: Color,
    val borderSecondary: Color,
    val fillSelection: Color,
    val fillMuted: Color,
    val fillOverlay: Color,
    val fillDisabled: Color,
    val tableRowEven: Color,
    val tableRowOdd: Color,
)

public object SimAnalyzerTheme {

    public val fonts: SimAnalyzerFontFamilies = DefaultSimAnalyzerFontFamilies
    public val corners: SimAnalyzerCornerTokens = SimAnalyzerCornerTokens()
    public val typography: Typography = simAnalyzerTypography(fonts)
    public val extended: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
    public val material: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = extended.material
    public val chrome: SimAnalyzerChromeTokens
        @Composable
        @ReadOnlyComposable
        get() = SimAnalyzerChromeTokens(
            dividerSubtle = material.outlineVariant.copy(alpha = 0.2f),
            borderSubtle = material.outlineVariant.copy(alpha = 0.25f),
            borderStrong = material.outlineVariant.copy(alpha = 0.35f),
            borderEmphasis = material.outlineVariant.copy(alpha = 0.4f),
            borderInteractive = material.primary.copy(alpha = 0.55f),
            borderInteractiveStrong = material.primary.copy(alpha = 0.75f),
            borderSecondary = material.secondary.copy(alpha = 0.45f),
            fillSelection = material.primary.copy(alpha = 0.2f),
            fillMuted = material.surfaceVariant.copy(alpha = 0.15f),
            fillOverlay = material.surfaceVariant.copy(alpha = 0.6f),
            fillDisabled = material.background.copy(alpha = 0.6f),
            tableRowEven = material.surfaceVariant.copy(alpha = 0.18f),
            tableRowOdd = material.surfaceVariant.copy(alpha = 0.12f),
        )
    public val shapes: Shapes = Shapes(
        extraSmall = corners.compact,
        small = corners.control,
        medium = corners.field,
        large = corners.panel,
        extraLarge = corners.display,
    )
    public val horizontalGradient: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.horizontalGradient(
            0.0f to extended.gradient0,
            0.2f to extended.gradient20,
            0.4f to extended.gradient40,
            0.6f to extended.gradient60,
            0.8f to extended.gradient80,
            1.0f to extended.gradient100,
        )
}

@Composable
public fun SimAnalyzerTheme(themeMode: ThemeMode = ThemeMode.System, content: @Composable () -> Unit = {}) {
    val isSystemDark = isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemDark
    }

    val targetTheme = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val animatedTheme = targetTheme.animated()

    CompositionLocalProvider(
        LocalExtendedColors provides animatedTheme,
        LocalDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = animatedTheme.material,
            typography = SimAnalyzerTheme.typography,
            shapes = SimAnalyzerTheme.shapes,
            content = content,
        )
    }
}

private val ThemeColorSmoothSpring = spring<Color>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

@Composable
private fun animateColor(targetValue: Color): Color {
    val animatedColor by animateColorAsState(
        targetValue = targetValue,
        animationSpec = ThemeColorSmoothSpring,
        label = "themeColor",
    )
    return animatedColor
}

@Composable
private fun ExtendedColors.animated(): ExtendedColors = ExtendedColors(
    material = material.animated(),

    shadow = animateColor(shadow),
    shadowSecondary = animateColor(shadowSecondary),
    errorOutline = animateColor(errorOutline),

    highPriorityOutline = animateColor(highPriorityOutline),
    highPriorityContainer = animateColor(highPriorityContainer),
    onHighPriorityContainer = animateColor(onHighPriorityContainer),

    middlePriorityOutline = animateColor(middlePriorityOutline),
    middlePriorityContainer = animateColor(middlePriorityContainer),
    onMiddlePriorityContainer = animateColor(onMiddlePriorityContainer),

    lowPriorityOutline = animateColor(lowPriorityOutline),
    lowPriorityContainer = animateColor(lowPriorityContainer),
    onLowPriorityContainer = animateColor(onLowPriorityContainer),

    gradient0 = animateColor(gradient0),
    gradient20 = animateColor(gradient20),
    gradient40 = animateColor(gradient40),
    gradient60 = animateColor(gradient60),
    gradient80 = animateColor(gradient80),
    gradient100 = animateColor(gradient100),

    purple = animateColor(purple),
    pink = animateColor(pink),
    lightPink = animateColor(lightPink),
    red = animateColor(red),
    amber = animateColor(amber),
    yellow = animateColor(yellow),
    lightGreen = animateColor(lightGreen),
    teal = animateColor(teal),
    cyan = animateColor(cyan),
    orange = animateColor(orange),

    surface50 = animateColor(surface50),
    onPrimaryContainer50 = animateColor(onPrimaryContainer50),
    onSecondaryContainer50 = animateColor(onSecondaryContainer50),
)

@Composable
private fun ColorScheme.animated(): ColorScheme = copy(
    primary = animateColor(primary),
    onPrimary = animateColor(onPrimary),
    primaryContainer = animateColor(primaryContainer),
    onPrimaryContainer = animateColor(onPrimaryContainer),

    secondary = animateColor(secondary),
    onSecondary = animateColor(onSecondary),
    secondaryContainer = animateColor(secondaryContainer),
    onSecondaryContainer = animateColor(onSecondaryContainer),

    tertiary = animateColor(tertiary),
    onTertiary = animateColor(onTertiary),
    tertiaryContainer = animateColor(tertiaryContainer),
    onTertiaryContainer = animateColor(onTertiaryContainer),

    error = animateColor(error),
    onError = animateColor(onError),
    errorContainer = animateColor(errorContainer),
    onErrorContainer = animateColor(onErrorContainer),

    background = animateColor(background),
    onBackground = animateColor(onBackground),

    surface = animateColor(surface),
    onSurface = animateColor(onSurface),
    surfaceVariant = animateColor(surfaceVariant),
    onSurfaceVariant = animateColor(onSurfaceVariant),

    outline = animateColor(outline),
    outlineVariant = animateColor(outlineVariant),

    inverseSurface = animateColor(inverseSurface),
    inverseOnSurface = animateColor(inverseOnSurface),
    inversePrimary = animateColor(inversePrimary),
)
