package com.project.analyzer.fuel.domain.predictor

import com.project.analyzer.fuel.data.model.SavedFuelData
import com.project.analyzer.fuel.domain.model.FuelEstimate
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.math.Ewma
import com.project.analyzer.math.MathEps.EPS_9_DOUBLE
import com.project.analyzer.telemetry.ac.api.contract.LapValidity
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import kotlin.math.max
import kotlin.math.min

class FuelConsumptionEngine(
    private val config: FuelConsumptionConfig,
) {

    private val tuning: FuelConsumptionTuning
        get() = config.tuning

    private var phase: FuelPhase = FuelPhase.PIT_WAITING
    private var hasExitedPits: Boolean = false

    private var lastTimestampNs: Long = 0L
    private var warmupStartNs: Long = 0L

    private var lastFuelLiters: Double? = null
    private var lapStartFuelLiters: Double? = null

    private var lastCompletedLaps: Int = -1
    private var validLapSamples: Int = 0
    private var lapFuelEwmaLitersPerLap: Double? = null

    private val fuelRateLpsEwma = Ewma(tauSec = tuning.tauFuelRateSec)
    private val speedKmhEwma = Ewma(tauSec = tuning.tauSpeedSec)
    private val lapTimeSecFromSpeedEwma = Ewma(tauSec = tuning.tauLapTimeSec)

    /** Moving distance since pit exit / last refuel (meters). */
    private var movingDistanceM: Double = 0.0

    /** Moving time since pit exit / last refuel (seconds). */
    private var movingTimeSec: Double = 0.0

    /** Fuel burned while moving since pit exit / last refuel (liters). */
    private var movingFuelBurnedLiters: Double = 0.0

    /** Time accumulator between discrete fuel updates (seconds). */
    private var secondsSinceFuelTick: Double = 0.0

    private var cachedCarModel: String? = null
    private var cachedTrackId: String? = null
    private var cachedTrackLengthM: Double? = null

    fun onFrame(frame: TelemetryFrame, savedFuelData: SavedFuelData? = null): FuelEstimate? {
        val fuelNow = frame.car?.fuel?.fuelLiters?.toDouble() ?: return null
        if (!fuelNow.isValidFuelLevel()) return null

        val speedKmh = (frame.car?.speedKmh?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val timestampNs = frame.timestampNs

        frame.session?.car?.carModel?.let { cachedCarModel = it }
        frame.session?.track?.trackId?.let { cachedTrackId = it }
        frame.session?.track?.lengthMeters
            ?.toDouble()
            ?.takeIf { it > 0.0 }
            ?.let { cachedTrackLengthM = it }

        val gameFuelPerLap = frame.car?.fuel?.fuelPerLapLiters
        val gameFuelEstimatedLaps = frame.car?.fuel?.fuelEstimatedLaps
        val maxFuel = frame.car?.fuel?.maxFuelLiters?.toDouble()

        val dtSecRaw = computeDeltaTimeSec(timestampNs) ?: run {
            initializeFuelState(fuelNow)
            return buildEstimate(
                frame = frame,
                savedFuelData = savedFuelData,
                currentFuel = fuelNow,
                maxFuel = maxFuel,
                gameFuelPerLap = gameFuelPerLap,
                gameFuelEstimatedLaps = gameFuelEstimatedLaps,
            )
        }

        if (dtSecRaw > tuning.dtMaxSec) {
            lastFuelLiters = fuelNow
            secondsSinceFuelTick = 0.0

            movingDistanceM = 0.0
            movingTimeSec = 0.0
            movingFuelBurnedLiters = 0.0

            fuelRateLpsEwma.reset()
            speedKmhEwma.reset()
            lapTimeSecFromSpeedEwma.reset()

            return buildEstimate(
                frame = frame,
                savedFuelData = savedFuelData,
                currentFuel = fuelNow,
                maxFuel = maxFuel,
                gameFuelPerLap = gameFuelPerLap,
                gameFuelEstimatedLaps = gameFuelEstimatedLaps,
            )
        }

        val prevFuel = lastFuelLiters ?: fuelNow
        lastFuelLiters = fuelNow

        if (fuelNow - prevFuel > tuning.refuelThresholdLiters) {
            handleRefuel(fuelNow, timestampNs)
            return buildEstimate(
                frame = frame,
                savedFuelData = savedFuelData,
                currentFuel = fuelNow,
                maxFuel = maxFuel,
                gameFuelPerLap = gameFuelPerLap,
                gameFuelEstimatedLaps = gameFuelEstimatedLaps,
            )
        }

        val fuelConsumed = max(0.0, prevFuel - fuelNow)
        val isMoving = speedKmh >= tuning.movingMinSpeedKmh

        val dtSecForEwma = min(dtSecRaw, tuning.dtMaxSec)

        if (isMoving) {
            speedKmhEwma.update(dtSecForEwma, speedKmh)

            val distanceM = (speedKmh / KMH_PER_MPS) * dtSecRaw
            movingDistanceM += distanceM

            secondsSinceFuelTick += dtSecRaw
            if (fuelConsumed > 0.0) {
                val windowSec = max(secondsSinceFuelTick, MIN_POSITIVE_WINDOW_SEC)
                val rateLps = fuelConsumed / windowSec
                if (rateLps <= tuning.maxPlausibleRateLps) {
                    fuelRateLpsEwma.update(windowSec, rateLps)
                }
                secondsSinceFuelTick = 0.0
            }
        }

        if (phase == FuelPhase.WARMUP || (phase == FuelPhase.PREDICTIVE && lapFuelEwmaLitersPerLap == null)) {
            movingTimeSec += dtSecRaw
            movingFuelBurnedLiters += fuelConsumed
        }

        updateLapTimeFromSpeed(dtSecForEwma)
        updatePhase(speedKmh, timestampNs)
        updatePerLapFuel(frame)

        return buildEstimate(
            frame = frame,
            savedFuelData = savedFuelData,
            currentFuel = fuelNow,
            maxFuel = maxFuel,
            gameFuelPerLap = gameFuelPerLap,
            gameFuelEstimatedLaps = gameFuelEstimatedLaps,
        )
    }

    fun reset() {
        phase = FuelPhase.PIT_WAITING
        hasExitedPits = false

        lastTimestampNs = 0L
        warmupStartNs = 0L

        lastFuelLiters = null
        lapStartFuelLiters = null

        lastCompletedLaps = -1
        validLapSamples = 0
        lapFuelEwmaLitersPerLap = null

        fuelRateLpsEwma.reset()
        speedKmhEwma.reset()
        lapTimeSecFromSpeedEwma.reset()

        movingDistanceM = 0.0
        movingTimeSec = 0.0
        movingFuelBurnedLiters = 0.0
        secondsSinceFuelTick = 0.0

        cachedCarModel = null
        cachedTrackId = null
        cachedTrackLengthM = null
    }

    private fun initializeFuelState(fuelNow: Double) {
        lastFuelLiters = fuelNow
        lapStartFuelLiters = fuelNow
        secondsSinceFuelTick = 0.0
    }

    private fun Double.isValidFuelLevel(): Boolean {
        return isFinite() &&
            this >= tuning.minPlausibleFuelLiters &&
            this <= tuning.maxPlausibleFuelLiters
    }

    private fun computeDeltaTimeSec(nowNs: Long): Double? {
        if (nowNs <= 0L) return null

        if (lastTimestampNs !in 1..<nowNs) {
            lastTimestampNs = nowNs
            return null
        }

        val dt = (nowNs - lastTimestampNs).toDouble() * EPS_9_DOUBLE
        lastTimestampNs = nowNs

        return dt.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun handleRefuel(newFuel: Double, nowNs: Long) {
        lastFuelLiters = newFuel
        lapStartFuelLiters = newFuel

        fuelRateLpsEwma.reset()
        secondsSinceFuelTick = 0.0

        movingDistanceM = 0.0
        movingTimeSec = 0.0
        movingFuelBurnedLiters = 0.0

        lapTimeSecFromSpeedEwma.reset()

        lastCompletedLaps = -1
        validLapSamples = 0
        lapFuelEwmaLitersPerLap = null

        if (phase != FuelPhase.PIT_WAITING) {
            phase = FuelPhase.WARMUP
            warmupStartNs = nowNs
        }
    }

    private fun updatePhase(speedKmh: Double, nowNs: Long) {
        when (phase) {
            FuelPhase.PIT_WAITING -> {
                if (!hasExitedPits && speedKmh >= tuning.pitExitSpeedKmh) {
                    hasExitedPits = true
                    phase = FuelPhase.WARMUP
                    warmupStartNs = nowNs

                    movingDistanceM = 0.0
                    movingTimeSec = 0.0
                    movingFuelBurnedLiters = 0.0
                    secondsSinceFuelTick = 0.0

                    lapStartFuelLiters = lastFuelLiters
                }
            }

            FuelPhase.WARMUP -> {
                val elapsedSec = (nowNs - warmupStartNs).toDouble() * EPS_9_DOUBLE
                if (elapsedSec >= tuning.warmupDurationSec) {
                    phase = FuelPhase.PREDICTIVE
                }
            }

            FuelPhase.PREDICTIVE -> {
                if (lapFuelEwmaLitersPerLap != null && validLapSamples >= tuning.minValidLapsForPerLap) {
                    phase = FuelPhase.PER_LAP
                }
            }

            FuelPhase.PER_LAP -> Unit
        }
    }

    private fun updatePerLapFuel(frame: TelemetryFrame) {
        val completedLaps = frame.lap?.completedLaps ?: frame.session?.completedLaps ?: 0

        // First time seeing lap data - just initialize, don't measure
        if (lastCompletedLaps < 0) {
            lapStartFuelLiters = lastFuelLiters
            lastCompletedLaps = completedLaps
            return
        }

        val deltaLaps = completedLaps - lastCompletedLaps
        if (deltaLaps <= 0) {
            return
        }

        // Skip the out-lap (first crossing from completedLaps 0 -> 1)
        if (lastCompletedLaps == 0 && completedLaps == 1) {
            lapStartFuelLiters = lastFuelLiters
            lastCompletedLaps = completedLaps
            return
        }

        val currentFuel = lastFuelLiters
        val startFuel = lapStartFuelLiters
        if (startFuel != null && currentFuel != null) {
            val fuelUsedTotal = startFuel - currentFuel
            val fuelUsedPerLap = fuelUsedTotal / deltaLaps.toDouble()

            if (fuelUsedPerLap in tuning.minFuelConsumedPerLap..tuning.maxPlausibleLitersPerLap) {
                repeat(deltaLaps) {
                    val cur = lapFuelEwmaLitersPerLap
                    lapFuelEwmaLitersPerLap = if (cur == null) {
                        fuelUsedPerLap
                    } else {
                        cur + tuning.lapFuelEwmaAlpha * (fuelUsedPerLap - cur)
                    }
                    validLapSamples++
                }
            }
        }

        lapStartFuelLiters = currentFuel
        lastCompletedLaps = completedLaps
    }

    private fun updateLapTimeFromSpeed(dtSec: Double) {
        val trackLenM = cachedTrackLengthM ?: return
        if (trackLenM < tuning.minTrackLengthMeters) return

        val avgSpeedKmh = speedKmhEwma.value
        if (avgSpeedKmh < tuning.movingMinSpeedKmh) return

        val speedMps = (avgSpeedKmh / KMH_PER_MPS).coerceAtLeast(MIN_SPEED_MPS_FOR_LAPTIME)
        val lapTimeSec = trackLenM / speedMps

        val clamped = lapTimeSec.coerceIn(tuning.minLapTimeSec, tuning.maxLapTimeSec)
        lapTimeSecFromSpeedEwma.update(dtSec, clamped)
    }

    private fun buildEstimate(
        frame: TelemetryFrame,
        savedFuelData: SavedFuelData?,
        currentFuel: Double,
        maxFuel: Double?,
        gameFuelPerLap: Float?,
        gameFuelEstimatedLaps: Float?,
    ): FuelEstimate {
        val (lapTimeSec, isFromCompletedLap) = estimateLapTimeSec(frame, savedFuelData)
        val litersPerLap = estimateLitersPerLap(
            savedFuelData = savedFuelData,
            gameFuelPerLap = gameFuelPerLap,
            lapTimeSec = lapTimeSec,
        )

        val litersPerSecond = fuelRateLpsEwma.value.takeIf { it > 0.0 }
        val lapsRemaining = when {
            gameFuelEstimatedLaps != null && gameFuelEstimatedLaps > 0 -> gameFuelEstimatedLaps.toDouble()
            litersPerLap != null && litersPerLap > 0 -> currentFuel / litersPerLap
            else -> null
        }

        val isCurrentLapValid = frame.lap?.validity?.let {
            it == LapValidity.VALID || it == LapValidity.UNKNOWN
        } ?: true

        return FuelEstimate(
            phase = phase,
            currentFuelLiters = currentFuel,
            maxFuelLiters = maxFuel,
            litersPerLap = litersPerLap,
            litersPerSecond = litersPerSecond,
            lastLapTimeMs = frame.lap?.lastLapTimeMs,
            estimatedLapTimeSec = lapTimeSec,
            isLapTimeFromCompletedLap = isFromCompletedLap,
            lapsRemaining = lapsRemaining,
            gameFuelPerLap = gameFuelPerLap,
            gameFuelEstimatedLaps = gameFuelEstimatedLaps,
            carModel = cachedCarModel,
            trackId = cachedTrackId,
            completedLaps = lastCompletedLaps,
            confidence = estimateConfidence(),
            isCurrentLapValid = isCurrentLapValid,
        )
    }

    private fun estimateLapTimeSec(frame: TelemetryFrame, savedFuelData: SavedFuelData?): Pair<Double?, Boolean> {
        val lapMs = frame.lap?.lastLapTimeMs ?: savedFuelData?.bestValidLapTimeMs
        if (lapMs != null && lapMs > 0) {
            val sec = lapMs / MS_PER_SECOND
            if (sec in tuning.minLapTimeSec..tuning.maxLapTimeSec) {
                return sec to true
            }
        }

        val fromSpeed = lapTimeSecFromSpeedEwma.value
        if (fromSpeed > 0.0) return fromSpeed to false

        return tuning.fallbackLapTimeSec to false
    }

    private fun estimateLitersPerLap(
        savedFuelData: SavedFuelData?,
        gameFuelPerLap: Float?,
        lapTimeSec: Double?,
    ): Double? {
        // 1) Game-provided value (fallback, often inaccurate)
        gameFuelPerLap
            ?.toDouble()
            ?.takeIf { it in MIN_PLAUSIBLE_LITERS_PER_LAP..tuning.maxPlausibleLitersPerLap }
            ?.let {
                return it
            }

        // 2) Our own completed laps calculation (EWMA) - most accurate
        lapFuelEwmaLitersPerLap
            ?.takeIf { it > 0.0 }
            ?.let {
                return it
            }

        // 4) Predictive from moving fuel / distance (matured).
        val predictiveFromDistance = estimatePredictiveFromDistance()
        if (predictiveFromDistance != null) {
            return predictiveFromDistance
        }

        // 3) Predictive from rate * lap time.
        val t = lapTimeSec ?: 0.0
        val predictiveFromRate = estimatePredictiveFromRate(t, savedFuelData)
        if (predictiveFromRate != null) {
            return predictiveFromRate
        }

        // 5) Saved baseline (last resort).
        return savedFuelData?.peakLitersPerLap
            ?.takeIf { it.isFinite() && it in MIN_PLAUSIBLE_LITERS_PER_LAP..tuning.maxPlausibleLitersPerLap }
    }

    private fun estimatePredictiveFromRate(lapTimeSec: Double, savedFuelData: SavedFuelData?): Double? {
        if (lapTimeSec <= 0.0) return null

        val hasEnoughFuelSignal =
            movingFuelBurnedLiters >= tuning.minFuelBurnedForEstimate || fuelRateLpsEwma.value > 0.0
        if (!hasEnoughFuelSignal) return null

        val avgRateLps = if (movingTimeSec > 0.0) (movingFuelBurnedLiters / movingTimeSec) else 0.0
        val ewmaRateLps = fuelRateLpsEwma.value

        val blendedRate = when {
            avgRateLps > 0.0 && ewmaRateLps > 0.0 ->
                BLEND_RATE_WEIGHT * avgRateLps + (1.0 - BLEND_RATE_WEIGHT) * ewmaRateLps

            avgRateLps > 0.0 -> avgRateLps
            else -> ewmaRateLps
        }

        if (blendedRate <= 0.0) return null

        val raw = blendedRate * lapTimeSec
        val plausible = raw.takeIf { it in MIN_PLAUSIBLE_LITERS_PER_LAP..tuning.maxPlausibleLitersPerLap }
            ?: return null

        val saved = savedFuelData?.peakLitersPerLap
        return if (saved != null && phase != FuelPhase.PER_LAP) {
            min(plausible, saved * SAVED_BASELINE_CAP_MULTIPLIER)
        } else {
            plausible
        }
    }

    private fun estimatePredictiveFromDistance(): Double? {
        val trackLenM = cachedTrackLengthM ?: return null
        if (trackLenM < tuning.minTrackLengthMeters) return null

        val minDistM = if (phase == FuelPhase.WARMUP) {
            MIN_DISTANCE_WARMUP_M
        } else {
            MIN_DISTANCE_PREDICTIVE_M
        }

        if (movingDistanceM < minDistM) return null
        if (movingFuelBurnedLiters < tuning.minFuelBurnedForEstimate) return null

        val litersPerLap = (movingFuelBurnedLiters / movingDistanceM) * trackLenM
        return litersPerLap.takeIf { it in MIN_PLAUSIBLE_LITERS_PER_LAP..tuning.maxPlausibleLitersPerLap }
    }

    private fun estimateConfidence(): Double {
        return when (phase) {
            FuelPhase.PIT_WAITING -> 0.0

            FuelPhase.WARMUP -> {
                val elapsedSec = (lastTimestampNs - warmupStartNs).toDouble() * EPS_9_DOUBLE
                val progress = (elapsedSec / tuning.warmupDurationSec).coerceIn(0.0, 1.0)
                progress * tuning.predictiveConfMin
            }

            FuelPhase.PREDICTIVE -> {
                val distanceFactor = min(1.0, movingDistanceM / CONF_DISTANCE_SCALE_M)
                tuning.predictiveConfMin +
                    (tuning.predictiveConfMax - tuning.predictiveConfMin) * distanceFactor
            }

            FuelPhase.PER_LAP -> {
                when {
                    validLapSamples >= 2 -> tuning.perLapConf2Plus
                    validLapSamples == 1 -> tuning.perLapConf1Lap
                    else -> tuning.predictiveConfMax
                }
            }
        }
    }

    private companion object {

        // Unit conversions
        const val KMH_PER_MPS = 3.6
        const val MS_PER_SECOND = 1_000.0

        // Numeric safety / guards
        const val MIN_POSITIVE_WINDOW_SEC = 1e-6
        const val MIN_SPEED_MPS_FOR_LAPTIME = 0.1

        // Plausibility (generic)
        const val MIN_PLAUSIBLE_LITERS_PER_LAP = 0.1

        // Predictive distance thresholds (meters)
        const val MIN_DISTANCE_WARMUP_M = 200.0
        const val MIN_DISTANCE_PREDICTIVE_M = 800.0

        // Predictive rate blending
        const val BLEND_RATE_WEIGHT = 0.5

        // Saved baseline guard (optional cap for predictive spikes)
        const val SAVED_BASELINE_CAP_MULTIPLIER = 1.40

        // Confidence scaling
        const val CONF_DISTANCE_SCALE_M = 10_000.0
    }
}
