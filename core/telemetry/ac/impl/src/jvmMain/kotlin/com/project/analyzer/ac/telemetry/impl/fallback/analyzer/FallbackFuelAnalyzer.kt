package com.project.analyzer.ac.telemetry.impl.fallback.analyzer

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.FuelSnapshot
import dev.zacsweers.metro.Inject

@Inject
class FallbackFuelAnalyzer {

    private var lastFuelLiters: Float? = null
    private var lapStartFuelLiters: Float? = null
    private var lastCompletedLaps: Int = -1
    private var lastStartFinishSyncId: Int? = null
    private var fuelPerLapEwma: Float? = null
    private var validLapCount: Int = 0
    private var snapshot: FuelSnapshot = FuelSnapshot()

    fun reset() {
        lastFuelLiters = null
        lapStartFuelLiters = null
        lastCompletedLaps = -1
        lastStartFinishSyncId = null

        fuelPerLapEwma = null
        validLapCount = 0
        snapshot = FuelSnapshot()
    }

    fun resetLapTracking(
        fuelLiters: Float,
        completedLaps: Int,
        startFinishSyncId: Int,
    ) {
        lastFuelLiters = fuelLiters
        lapStartFuelLiters = fuelLiters
        lastCompletedLaps = completedLaps
        lastStartFinishSyncId = startFinishSyncId
    }

    fun processFrame(
        fuelLiters: Float,
        completedLaps: Int,
        lastLapTimeMs: Int,
        startFinishSyncId: Int,
    ) {
        val prevSyncId = lastStartFinishSyncId
        if (prevSyncId == null) {
            lastStartFinishSyncId = startFinishSyncId
        } else if (startFinishSyncId != prevSyncId) {
            lastStartFinishSyncId = startFinishSyncId
            lastFuelLiters = fuelLiters
            lapStartFuelLiters = fuelLiters
            lastCompletedLaps = completedLaps
            return
        }

        if (lastFuelLiters == null) {
            lastFuelLiters = fuelLiters
            lapStartFuelLiters = fuelLiters
            lastCompletedLaps = completedLaps
            return
        }

        val prevFuel = lastFuelLiters ?: fuelLiters
        lastFuelLiters = fuelLiters

        if (fuelLiters - prevFuel > REFUEL_THRESHOLD_LITERS) {
            lapStartFuelLiters = fuelLiters
            lastCompletedLaps = completedLaps
            return
        }

        if (lastCompletedLaps >= 0 && completedLaps < lastCompletedLaps) {
            lapStartFuelLiters = fuelLiters
            lastCompletedLaps = completedLaps
            return
        }

        if (lastCompletedLaps < 0) {
            lastCompletedLaps = completedLaps
            lapStartFuelLiters = fuelLiters
            return
        }

        val deltaLaps = completedLaps - lastCompletedLaps
        if (deltaLaps <= 0) return

        val canCalculate = lastLapTimeMs > 0
        val startFuel = lapStartFuelLiters

        if (canCalculate && startFuel != null) {
            val fuelUsedTotal = startFuel - fuelLiters
            val fuelUsedPerLap = fuelUsedTotal / deltaLaps

            if (fuelUsedPerLap in MIN_FUEL_PER_LAP..MAX_FUEL_PER_LAP) {
                val effectiveAlpha = calculateEffectiveAlpha(deltaLaps)
                val cur = fuelPerLapEwma
                fuelPerLapEwma = if (cur == null) {
                    fuelUsedPerLap
                } else {
                    cur + effectiveAlpha * (fuelUsedPerLap - cur)
                }
                validLapCount += deltaLaps

                updateSnapshot(fuelLiters, lastLapRawPerLap = fuelUsedPerLap)
            }
        }

        lapStartFuelLiters = fuelLiters
        lastCompletedLaps = completedLaps
    }

    private fun calculateEffectiveAlpha(lapCount: Int): Float {
        if (lapCount <= 1) return EWMA_ALPHA

        var complement = 1f - EWMA_ALPHA
        repeat(lapCount - 1) {
            complement *= (1f - EWMA_ALPHA)
        }
        return (1f - complement).coerceIn(EWMA_ALPHA, MAX_EFFECTIVE_ALPHA)
    }

    private fun updateSnapshot(currentFuel: Float, lastLapRawPerLap: Float) {
        val ewma = fuelPerLapEwma ?: return

        val estimatedLaps = if (ewma > 0f) currentFuel / ewma else null

        snapshot = FuelSnapshot(
            fuelPerLapLiters = ewma,
            lastLapFuelPerLapLiters = lastLapRawPerLap,
            fuelPerLapEwmaLiters = ewma,
            fuelEstimatedLaps = estimatedLaps,
        )
    }

    fun getSnapshot(currentFuelLiters: Float): FuelSnapshot {
        val perLap = fuelPerLapEwma ?: return snapshot
        return if (perLap > 0f) {
            snapshot.copy(fuelEstimatedLaps = currentFuelLiters / perLap)
        } else {
            snapshot
        }
    }

    private companion object {

        const val REFUEL_THRESHOLD_LITERS = 0.5f
        const val MIN_FUEL_PER_LAP = 0.001f
        const val MAX_FUEL_PER_LAP = 50f
        const val EWMA_ALPHA = 0.35f
        const val MAX_EFFECTIVE_ALPHA = 0.85f
    }
}
