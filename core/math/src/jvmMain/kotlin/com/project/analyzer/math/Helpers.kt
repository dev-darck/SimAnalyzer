package com.project.analyzer.math

import com.project.analyzer.math.MathEps.EPS_6_DOUBLE

public fun Double?.finiteOrNull(): Double? = this?.takeIf { it.isFinite() }

public fun Double?.finiteNonNegativeOrNull(): Double? = this?.takeIf { it.isFinite() && it >= 0.0 }

public fun Double?.finitePositiveOrNull(min: Double = EPS_6_DOUBLE): Double? =
    this?.takeIf { it.isFinite() && it > min }

public fun clamp(x: Double, min: Double, max: Double): Double = x.coerceIn(min, max)
public fun clamp01(x: Double): Double = x.coerceIn(0.0, 1.0)
public fun Double.coerceNonNegative(): Double = if (this < 0.0) 0.0 else this

/** curr - prev, only if > eps. */
public fun positiveIncrease(curr: Double?, prev: Double?, eps: Double = EPS_6_DOUBLE): Double? {
    if (curr == null || prev == null) return null
    val d = curr - prev
    return d.takeIf { it.isFinite() && it > eps }
}

/** prev - curr, only if > eps (i.e. drop). */
public fun positiveDecrease(prev: Double?, curr: Double?, eps: Double = EPS_6_DOUBLE): Double? {
    if (prev == null || curr == null) return null
    val d = prev - curr
    return d.takeIf { it.isFinite() && it > eps }
}

public fun safeDiv(num: Double, den: Double, eps: Double = EPS_6_DOUBLE): Double? {
    if (!num.isFinite() || !den.isFinite()) return null
    if (kotlin.math.abs(den) <= eps) return null
    return num / den
}

/** Simple EWMA step (alpha in [0..1]). */
public fun ewma(prev: Double?, sample: Double, alpha: Double): Double {
    val a = alpha.coerceIn(0.0, 1.0)
    return if (prev == null) sample else prev + (sample - prev) * a
}
