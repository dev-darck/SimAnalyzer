package com.project.analyzer.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFFF5F7FA)
private val OnBackground = Color(0xFF101922)

private val Surface = Color(0xFFFFFFFF)
private val OnSurface = Color(0xFF101922)
private val Surface50 = Color(0xFF101922).copy(alpha = 0.5f)

private val SurfaceVariant = Color(0xFFE8EEF6)
private val OnSurfaceVariant = Color(0xFF5A6A7A)

private val Primary = Color(0xFF2B8CEE)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF2B8CEE)
private val OnPrimaryContainer = Color(0xFFFFFFFF)
private val OnPrimaryContainer50 = Color(0xFFFFFFFF).copy(alpha = 0.5f)

private val Secondary = Color(0xFF2B8CEE).copy(alpha = 0.15f)
private val OnSecondary = Color(0xFF2B8CEE)
private val SecondaryContainer = Color(0xFF2B8CEE).copy(alpha = 0.08f)
private val OnSecondaryContainer = Color(0xFF0A5CAD)
private val OnSecondaryContainer50 = Color(0xFF0A5CAD).copy(alpha = 0.5f)

private val Purple = Color(0xFF7C3AED)
private val Pink = Color(0xFFDB2777)
private val LightPink = Color(0xFFEC4899)
private val Red = Color(0xFFE11D48)

private val Amber = Color(0xFFD97706)
private val Yellow = Color(0xFFCA8A04)
private val LightGreen = Color(0xFF65A30D)
private val Teal = Color(0xFF0D9488)
private val Cyan = Color(0xFF0891B2)
private val Orange = Color(0xFFEA580C)

private val Tertiary = Teal
private val OnTertiary = Color(0xFFFFFFFF)
private val TertiaryContainer = Color(0xFFCCFBF1)
private val OnTertiaryContainer = Color(0xFF134E4A)

private val Outline = Color(0xFFD1D9E6)
private val OutlineVariant = Color(0xFFE2E8F0)

private val Error = Color(0xFFDC2626)
private val OnError = Color(0xFFFFFFFF)
private val ErrorContainer = Color(0xFFFEE2E2)
private val OnErrorContainer = Color(0xFF991B1B)

private val InverseSurface = Color(0xFF101922)
private val InverseOnSurface = Color(0xFFFFFFFF)
private val InversePrimary = Color(0xFF60A5FA)

private val Shadow = Color(0xFF000000).copy(alpha = 0.1f)
private val ShadowSecondary = Color(0xFF101922).copy(alpha = 0.05f)

private val ErrorOutline = Color(0xFFDC2626)

private val HighPriorityOutline = Color(0xFFDC2626)
private val HighPriorityContainer = Color(0xFFFEE2E2)
private val OnHighPriorityContainer = Color(0xFF991B1B)

private val MiddlePriorityOutline = Color(0xFFD97706)
private val MiddlePriorityContainer = Color(0xFFFEF3C7)
private val OnMiddlePriorityContainer = Color(0xFF92400E)

private val LowPriorityOutline = Color(0xFF0D9488)
private val LowPriorityContainer = Color(0xFFCCFBF1)
private val OnLowPriorityContainer = Color(0xFF134E4A)

private val Gradient0 = Teal
private val Gradient20 = LightGreen
private val Gradient40 = LightGreen
private val Gradient60 = Amber
private val Gradient80 = Orange
private val Gradient100 = Red

public val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = Background,
    onBackground = OnBackground,

    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    outline = Outline,
    outlineVariant = OutlineVariant,

    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary,
)

internal val LightExtendedColors = ExtendedColors(
    material = LightColorScheme,
    
    shadow = Shadow,
    shadowSecondary = ShadowSecondary,
    errorOutline = ErrorOutline,

    highPriorityOutline = HighPriorityOutline,
    highPriorityContainer = HighPriorityContainer,
    onHighPriorityContainer = OnHighPriorityContainer,

    middlePriorityOutline = MiddlePriorityOutline,
    middlePriorityContainer = MiddlePriorityContainer,
    onMiddlePriorityContainer = OnMiddlePriorityContainer,

    lowPriorityOutline = LowPriorityOutline,
    lowPriorityContainer = LowPriorityContainer,
    onLowPriorityContainer = OnLowPriorityContainer,

    gradient0 = Gradient0,
    gradient20 = Gradient20,
    gradient40 = Gradient40,
    gradient60 = Gradient60,
    gradient80 = Gradient80,
    gradient100 = Gradient100,

    purple = Purple,
    pink = Pink,
    lightPink = LightPink,
    red = Red,
    amber = Amber,
    yellow = Yellow,
    lightGreen = LightGreen,
    teal = Teal,
    cyan = Cyan,
    orange = Orange,

    surface50 = Surface50,
    onPrimaryContainer50 = OnPrimaryContainer50,
    onSecondaryContainer50 = OnSecondaryContainer50,
)
