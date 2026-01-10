package com.project.analyzer.math

import com.project.analyzer.math.MathEps.EPS_3_DOUBLE
import com.project.analyzer.math.MathEps.EPS_4_DOUBLE
import com.project.analyzer.math.MathEps.EPS_9_DOUBLE

public fun Int.msToSec(): Double = this.toDouble() * EPS_3_DOUBLE
public fun Long.msToSec(): Double = this.toDouble() * EPS_3_DOUBLE
public fun Long.nsToSec(): Double = this.toDouble() * EPS_9_DOUBLE

/**
 * Pure dt computation without state.
 * Returns null if timestamps are invalid or prev == 0.
 */
public fun dtSecFromNs(
    nowNs: Long,
    prevNs: Long,
    minDtSec: Double = EPS_4_DOUBLE,
    maxDtSec: Double = 0.5,
): Double? {
    if (nowNs <= 0L || prevNs <= 0L) return null
    val dt = (nowNs - prevNs) * EPS_9_DOUBLE
    if (!dt.isFinite()) return null
    return dt.coerceIn(minDtSec, maxDtSec)
}
