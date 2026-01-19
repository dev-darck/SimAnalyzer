package com.project.analyzer.utils.ext

import java.util.Locale
import kotlin.math.abs

/**
 * Formats lap delta time (e.g., "+1.234" or "-0.567")
 */
public fun formatDeltaTime(deltaMs: Int?, isPositive: Boolean?): String {
    if (deltaMs == null) return "+0.000"
    val sign = if (isPositive == true) "+" else "-"
    val sec = abs(deltaMs) / 1000.0
    return String.format(Locale.US, "$sign%.3f", sec)
}

/**
 * Formats sector time in seconds (e.g., "32.45")
 */
public fun Int.formatSectorTime(): String {
    if (this <= 0) return "--.--"
    return String.format(Locale.US, "%.2f", this / 1000.0)
}

/**
 * Checks if tyre pressure is in optimal range for GT3 cars
 * Optimal: 27-28 psi, Acceptable: 26-29 psi
 */
public fun Float.isTyrePressureOptimal(
    optimalMin: Float = 26f,
    optimalMax: Float = 29f
): Boolean = this in optimalMin..optimalMax

/**
 * Determines tyre temperature zone based on core temp
 * GT3 optimal range: 80-100°C
 */
public enum class TyreTemperatureZone {

    COLD,      // < 70°C - blue
    WARMING,   // 70-80°C - cyan
    OPTIMAL,   // 80-100°C - green
    HOT,       // 100-110°C - yellow
    OVERHEAT   // > 110°C - red
}

public fun Float.getTyreTemperatureZone(): TyreTemperatureZone = when {
    this < 70f -> TyreTemperatureZone.COLD
    this < 80f -> TyreTemperatureZone.WARMING
    this < 100f -> TyreTemperatureZone.OPTIMAL
    this < 110f -> TyreTemperatureZone.HOT
    else -> TyreTemperatureZone.OVERHEAT
}

/**
 * Determines brake temperature zone
 * GT3 optimal range: 300-600°C
 */
public enum class BrakeTemperatureZone {

    COLD,      // < 200°C
    WARMING,   // 200-300°C
    OPTIMAL,   // 300-600°C
    HOT,       // 600-800°C
    OVERHEAT   // > 800°C
}

public fun Float.getBrakeTemperatureZone(): BrakeTemperatureZone = when {
    this < 200f -> BrakeTemperatureZone.COLD
    this < 300f -> BrakeTemperatureZone.WARMING
    this < 600f -> BrakeTemperatureZone.OPTIMAL
    this < 800f -> BrakeTemperatureZone.HOT
    else -> BrakeTemperatureZone.OVERHEAT
}

/**
 * Maps raw gear value from telemetry to display gear
 * Input: 0=reverse, 1=neutral, 2+=forward
 * Output: -1=R, 0=N, 1+=forward gears
 */
public fun Int.toDisplayGear(): Int = when (this) {
    0 -> -1
    1 -> 0
    else -> this - 1
}

/**
 * Calculates RPM scale for display (in thousands)
 */
public fun Int.toRpmScale(): Float = this / 1000f

/**
 * Calculates max RPM scale for gauge (rounded up to next thousand)
 */
public fun Int.toMaxRpmScale(): Int {
    if (this <= 0) return 10
    return (this / 1000) + 1
}
