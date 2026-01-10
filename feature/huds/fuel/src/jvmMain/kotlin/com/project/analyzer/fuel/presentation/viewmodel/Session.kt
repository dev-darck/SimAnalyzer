package com.project.analyzer.fuel.presentation.viewmodel

internal data class SessionKey(val carModel: String?, val trackId: String?)

internal data class PeakState(
    var peakLitersPerLap: Double? = null,
    val peakPlanFuelLiters: MutableList<Double?> = mutableListOf()
) {

    fun updatePeakLitersPerLap(value: Double?, maxValue: Double = 200.0) {
        if (value == null) return
        if (!value.isFinite() || value <= 0 || value > maxValue) return
        peakLitersPerLap = maxOf(peakLitersPerLap ?: 0.0, value)
    }

    fun updatePeakPlanFuel(index: Int, value: Double?, maxValue: Double = 10_000.0) {
        if (value == null) return
        if (!value.isFinite() || value <= 0 || value > maxValue) return

        while (peakPlanFuelLiters.size <= index) {
            peakPlanFuelLiters.add(null)
        }

        val current = peakPlanFuelLiters[index]
        peakPlanFuelLiters[index] = maxOf(current ?: 0.0, value)
    }

    fun reset() {
        peakLitersPerLap = null
        peakPlanFuelLiters.clear()
    }
}
