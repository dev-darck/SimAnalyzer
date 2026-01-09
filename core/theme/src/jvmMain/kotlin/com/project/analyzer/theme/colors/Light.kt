package com.project.analyzer.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFFF5F5F7)
private val OnBackground = Color(0xFF111418)

private val Surface = Color(0xFFFCFCFD)
private val OnSurface = Color(0xFF111418)

private val SurfaceVariant = Color(0xFFF3F4F6)
private val OnSurfaceVariant = Color(0xFF6B7280)

private val Primary = Color(0xFF2B8CEE)
public val OnPrimary: Color = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFD6E8FF)
private val OnPrimaryContainer = Color(0xFF00213A)

private val Secondary = Color(0xFF3B4A5C)
private val OnSecondary = Color(0xFFFFFFFF)
private val SecondaryContainer = Color(0xFFE8EEF6)
private val OnSecondaryContainer = Color(0xFF111418)

private val Tertiary = Color(0xFF10B981)
private val OnTertiary = Color(0xFFFFFFFF)
private val TertiaryContainer = Color(0xFFD1FAE5)
private val OnTertiaryContainer = Color(0xFF064E3B)

private val Outline = Color(0xFFE5E7EB)
private val OutlineVariant = Color(0xFFCBD5E1)

private val Error = Color(0xFFBA1A1A)
private val OnError = Color(0xFFFFFFFF)
private val ErrorContainer = Color(0xFFFFDAD6)
private val OnErrorContainer = Color(0xFF410002)

private val InverseSurface = Color(0xFF101922)
private val InverseOnSurface = Color(0xFFE9EEF7)
private val InversePrimary = Color(0xFF2B8CEE)

/** Extended (non-M3) colors used by the app */
internal val Shadow = Color(0xFF000000)
internal val ShadowSecondary = Color(0xFF111418)

internal val ErrorOutline = Color(0xFFBA1A1A)

internal val HighPriorityOutline = Color(0xFFEF4444)
internal val HighPriorityContainer = Color(0xFFFEE2E2)
internal val OnHighPriorityContainer = Color(0xFF7F1D1D)

internal val MiddlePriorityOutline = Color(0xFFF59E0B)
internal val MiddlePriorityContainer = Color(0xFFFEF3C7)
internal val OnMiddlePriorityContainer = Color(0xFF78350F)

internal val LowPriorityOutline = Color(0xFF10B981)
internal val LowPriorityContainer = Color(0xFFD1FAE5)
internal val OnLowPriorityContainer = Color(0xFF064E3B)

internal val GradientStart = Primary
internal val GradientEnd = Tertiary
internal val OnGradient = Color(0xFFFFFFFF)

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

    gradientStart = GradientStart,
    gradientEnd = GradientEnd,
    onGradient = OnGradient,

    material = LightColorScheme
)
