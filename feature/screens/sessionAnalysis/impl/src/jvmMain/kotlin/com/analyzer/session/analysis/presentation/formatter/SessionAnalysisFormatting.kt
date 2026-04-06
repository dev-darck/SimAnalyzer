package com.analyzer.session.analysis.presentation.formatter

import java.util.Locale

/**
 * Shared formatting keeps telemetry values readable and consistent across every session-analysis surface.
 */
internal fun formatLapTime(durationMs: Int): String {
    val minutes = durationMs / 60_000
    val seconds = (durationMs % 60_000) / 1_000
    val millis = durationMs % 1_000
    return "%d:%02d.%03d".format(minutes, seconds, millis)
}

internal fun formatDelta(deltaMs: Int): String {
    val prefix = if (deltaMs > 0) "+" else ""
    return prefix + "%.3f".format(deltaMs / 1000f)
}

internal fun formatDelta(deltaMs: Int?): String = deltaMs?.let(::formatDelta) ?: "--"

internal fun formatSpeed(speedKmh: Float?): String = speedKmh
    ?.let { value -> String.format(Locale.US, "%.0f km/h", value) }
    ?: "--"

internal fun formatPercent(value: Float?): String = value
    ?.let { fraction -> String.format(Locale.US, "%.0f%%", fraction.coerceIn(0f, 1f) * 100f) }
    ?: "--"

internal fun formatTemperature(valueC: Float?): String = valueC
    ?.let { value -> String.format(Locale.US, "%.0fC", value) }
    ?: "--"

internal fun formatPressure(valuePsi: Float?): String = valuePsi
    ?.let { value -> String.format(Locale.US, "%.1f psi", value) }
    ?: "--"

internal fun formatRadians(valueRad: Float?): String = valueRad
    ?.let { value -> String.format(Locale.US, "%.2f rad", value) }
    ?: "--"

internal fun formatDegrees(valueRad: Float?): String = valueRad
    ?.let { value -> String.format(Locale.US, "%.0fdeg", Math.toDegrees(value.toDouble())) }
    ?: "--"

internal fun formatG(value: Float?): String = value
    ?.let { amount -> String.format(Locale.US, "%.2f g", amount) }
    ?: "--"

internal fun formatYawRate(valueRad: Float?): String = valueRad
    ?.let { value -> String.format(Locale.US, "%.2f rad/s", value) }
    ?: "--"

internal fun formatRpm(value: Float?): String = value
    ?.let { amount -> String.format(Locale.US, "%.0f rpm", amount) }
    ?: "--"

internal fun formatFuel(valueLiters: Float?): String = valueLiters
    ?.let { amount -> String.format(Locale.US, "%.1f L", amount) }
    ?: "--"

internal fun formatGear(value: Int?): String = value
    ?.takeIf { gear -> gear >= 0 }
    ?.let { gear -> if (gear == 0) "N" else gear.toString() }
    ?: "--"

internal fun formatTrackPosition(value: Float?): String = value
    ?.let { amount -> String.format(Locale.US, "%.0f%%", amount.coerceIn(0f, 1f) * 100f) }
    ?: "--"

internal fun formatSampleIndex(sampleIndexInLap: Int): String = if (sampleIndexInLap > 0) {
    sampleIndexInLap.toString()
} else {
    "--"
}

internal fun averageOf(vararg values: Float?): Float? {
    val nonNullValues = values.filterNotNull()
    if (nonNullValues.isEmpty()) return null
    return nonNullValues.sum() / nonNullValues.size.toFloat()
}
