package com.project.analyzer.utils.ext

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Converts radians to degrees.
 */
public fun Float.toDegrees(): Float = (this * (180f / PI.toFloat()))

public fun Float.fmt(decimals: Int = 3): String =
    String.format(Locale.US, "%.${decimals}f", this)

public fun Float?.orZero(): Float = this ?: 0f

public fun Float.toSteerDegrees(
    invert: Boolean = false,
    deadZoneDeg: Float = 0.5f,
    clampAbsDeg: Float = 180f,
    wheelRotationDeg: Float = 900f,
): Float {
    if (!this.isFinite()) return 0f

    val v = if (invert) -this else this

    val (deg, clampDeg) = if (abs(v) <= 1.5f) {
        val half = wheelRotationDeg / 2f
        (v.coerceIn(-1f, 1f) * half) to half
    } else {
        (v.toDegrees()) to clampAbsDeg
    }

    val clamped = deg.coerceIn(-clampDeg, clampDeg)
    return if (abs(clamped) < deadZoneDeg) 0f else clamped
}

public fun Float.ratioToPercent0_100(): Float {
    if (!this.isFinite()) return 0f
    return when {
        this <= 1.5f -> (this * 100f).coerceIn(0f, 100f)
        else -> this.coerceIn(0f, 100f)
    }
}

public fun Float.wearToRemainingPercent(): Int {
    if (!this.isFinite()) return 0

    val wear01 = when {
        this <= 1.5f -> this.coerceIn(0f, 1f)
        this <= 100f -> (this / 100f).coerceIn(0f, 1f)
        else -> 1f
    }

    return ((1f - wear01) * 100f).roundToInt().coerceIn(0, 100)
}
