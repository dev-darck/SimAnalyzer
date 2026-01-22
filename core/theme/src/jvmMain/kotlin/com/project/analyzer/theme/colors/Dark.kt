package com.project.analyzer.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF101922)
private val OnBackground = Color(0xFFFFFFFF)

private val Surface = Color(0xFF182430)
private val OnSurface = Color(0xFFFFFFFF)
private val Surface50 = Color(0xFFFFFFFF)

private val SurfaceVariant = Color(0xFF252E3A)
private val OnSurfaceVariant = Color(0xFF9BA6B6)

private val Primary = Color(0xFF2B8CEE)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF2B8CEE)
private val OnPrimaryContainer = Color(0xFFFFFFFF)
private val OnPrimaryContainer50 = Color(0xFFFFFFFF).copy(alpha = 0.5f)

private val Secondary = Color(0xFF2B8CEE).copy(alpha = 0.3f)
private val OnSecondary = Color(0xFFFFFFFF)
private val SecondaryContainer = Color(0xFF2B8CEE).copy(alpha = 0.1f)
private val OnSecondaryContainer = Color(0xFFFFFFFF)
private val OnSecondaryContainer50 = Color(0xFFFFFFFF).copy(alpha = 0.5f)

private val Purple = Color(0xFF9333EA)
private val Pink = Color(0xFFC51162)
private val LightPink = Color(0xFFDD349F)
private val Red = Color(0xFFF43F5E)

private val Amber = Color(0xFFF59727)
private val Yellow = Color(0xFFEFCB1B)
private val LightGreen = Color(0xFF87C249)
private val Teal = Color(0xFF10B981)
private val Cyan = Color(0xFF00B8D4)
private val Orange = Color(0xFFF15C3C)

private val Tertiary = Teal
private val OnTertiary = Color(0xFF062017)
private val TertiaryContainer = Color(0xFF0B3D31)
private val OnTertiaryContainer = Color(0xFFB7F7DE)

private val Outline = Color(0xFF283039)
private val OutlineVariant = Color(0xFF3A4555)

private val Error = Red
private val OnError = Color(0xFFFFFFFF)
private val ErrorContainer = Color(0xFF93000A)
private val OnErrorContainer = Color(0xFFFFDAD6)

private val InverseSurface = Color(0xFFE9EEF7)
private val InverseOnSurface = Color(0xFF101922)
private val InversePrimary = Primary

private val DarkShadow = Color(0xFF000000)
private val DarkShadowSecondary = Color(0xFF000000)

private val DarkErrorOutline = Red

private val DarkHighPriorityOutline = Red
private val DarkHighPriorityContainer = Color(0xFF7F1D1D)
private val DarkOnHighPriorityContainer = Color(0xFFFEE2E2)

private val DarkMiddlePriorityOutline = Amber
private val DarkMiddlePriorityContainer = Color(0xFF78350F)
private val DarkOnMiddlePriorityContainer = Color(0xFFFEF3C7)

private val DarkLowPriorityOutline = Teal
private val DarkLowPriorityContainer = Color(0xFF064E3B)
private val DarkOnLowPriorityContainer = Color(0xFFD1FAE5)

private val Gradient0 = Teal
private val Gradient20 = LightGreen
private val Gradient40 = LightGreen
private val Gradient60 = Amber
private val Gradient80 = Orange
private val Gradient100 = Red

public val DarkColorScheme: ColorScheme = darkColorScheme(
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

internal val DarkExtendedColors = ExtendedColors(
    material = DarkColorScheme,
    
    shadow = DarkShadow,
    shadowSecondary = DarkShadowSecondary,
    errorOutline = DarkErrorOutline,

    highPriorityOutline = DarkHighPriorityOutline,
    highPriorityContainer = DarkHighPriorityContainer,
    onHighPriorityContainer = DarkOnHighPriorityContainer,

    middlePriorityOutline = DarkMiddlePriorityOutline,
    middlePriorityContainer = DarkMiddlePriorityContainer,
    onMiddlePriorityContainer = DarkOnMiddlePriorityContainer,

    lowPriorityOutline = DarkLowPriorityOutline,
    lowPriorityContainer = DarkLowPriorityContainer,
    onLowPriorityContainer = DarkOnLowPriorityContainer,

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
