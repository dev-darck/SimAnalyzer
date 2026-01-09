package com.project.analyzer.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF101922)
private val OnBackground = Color(0xFFFFFFFF)

private val Surface = Color(0xFF1A222C)
private val OnSurface = Color(0xFFFFFFFF)

private val SurfaceVariant = Color(0xFF252E3A)
private val OnSurfaceVariant = Color(0xFF9BA6B6)

private val Primary = Color(0xFF2B8CEE)
private val OnPrimaryDark = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFF16314E)
private val OnPrimaryContainer = Color(0xFFD6E8FF)

private val Secondary = Color(0xFF9BA6B6)
private val OnSecondary = Color(0xFF101922)
private val SecondaryContainer = Color(0xFF2F3845)
private val OnSecondaryContainer = Color(0xFFDDE6F1)

private val Tertiary = Color(0xFF10B981)
private val OnTertiary = Color(0xFF062017)
private val TertiaryContainer = Color(0xFF0B3D31)
private val OnTertiaryContainer = Color(0xFFB7F7DE)

private val Outline = Color(0xFF283039)
private val OutlineVariant = Color(0xFF3A4555)

private val Error = Color(0xFFFFB4AB)
private val OnError = Color(0xFF690005)
private val ErrorContainer = Color(0xFF93000A)
private val OnErrorContainer = Color(0xFFFFDAD6)

private val InverseSurface = Color(0xFFE9EEF7)
private val InverseOnSurface = Color(0xFF101922)
private val InversePrimary = Color(0xFF2B8CEE)

/** Extended (non-M3) colors used by the app (prefixed to avoid collisions with Light.kt symbols) */
private val DarkShadow = Color(0xFF000000)
private val DarkShadowSecondary = Color(0xFF000000)

private val DarkErrorOutline = Error

private val DarkHighPriorityOutline = Color(0xFFFCA5A5)
private val DarkHighPriorityContainer = Color(0xFF7F1D1D)
private val DarkOnHighPriorityContainer = Color(0xFFFEE2E2)

private val DarkMiddlePriorityOutline = Color(0xFFFCD34D)
private val DarkMiddlePriorityContainer = Color(0xFF78350F)
private val DarkOnMiddlePriorityContainer = Color(0xFFFEF3C7)

private val DarkLowPriorityOutline = Color(0xFF6EE7B7)
private val DarkLowPriorityContainer = Color(0xFF064E3B)
private val DarkOnLowPriorityContainer = Color(0xFFD1FAE5)

private val DarkGradientStart = Primary
private val DarkGradientEnd = Tertiary
private val DarkOnGradient = Color(0xFFFFFFFF)

public val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimaryDark,
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

    gradientStart = DarkGradientStart,
    gradientEnd = DarkGradientEnd,
    onGradient = DarkOnGradient,

    material = DarkColorScheme
)
