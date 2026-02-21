package com.project.analyzer.math

import kotlin.math.max

public class Ewma(
    private val tauSec: Double,
    private val initial: Double = 0.0,
    private val eps: Double = MathEps.EPS_6_DOUBLE,
) {
    public var value: Double = initial
        private set

    public fun update(dtSec: Double, sample: Double): Double {
        val dt = max(eps, dtSec)
        val alpha = dt / (tauSec + dt)
        value += (sample - value) * alpha
        return value
    }

    public fun reset() {
        value = initial
    }

    override fun toString(): String = "Ewma(value=$value, tauSec=$tauSec, initial=$initial)"
}
