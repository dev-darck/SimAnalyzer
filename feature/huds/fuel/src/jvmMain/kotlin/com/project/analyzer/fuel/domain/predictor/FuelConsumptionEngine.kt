package com.project.analyzer.fuel.domain.predictor

import com.project.analyzer.fuel.data.model.SavedFuelData
import com.project.analyzer.fuel.domain.model.FuelEstimate
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.math.Ewma
import com.project.analyzer.math.MathEps.EPS_9_DOUBLE
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import dev.zacsweers.metro.Inject
import kotlin.math.max
import kotlin.math.min

@Inject
internal class FuelConsumptionEngine {
    private val tuning: FuelConsumptionTuning
        get() = FuelConsumptionConfig.tuning

    // Phase tracking
    private var phase: FuelPhase = FuelPhase.PIT_WAITING
    private var hasExitedPits: Boolean = false

    // Timestamps
    private var lastTimestampNs: Long = 0L
    private var warmupStartNs: Long = 0L

    // Fuel tracking
    private var lastFuelLiters: Double? = null

    // EWMA filters
    private val fuelRateLpsEwma = Ewma(tauSec = tuning.tauFuelRateSec)
    private val speedKmhEwma = Ewma(tauSec = tuning.tauSpeedSec)
    private val lapTimeSecFromSpeedEwma = Ewma(tauSec = tuning.tauLapTimeSec)

    // Predictive accumulators
    private var movingDistanceM: Double = 0.0
    private var movingTimeSec: Double = 0.0
    private var movingFuelBurnedLiters: Double = 0.0
    private var secondsSinceFuelTick: Double = 0.0

    // Session info cache
    private var cachedCarModel: String? = null
    private var cachedTrackId: String? = null
    private var cachedTrackLengthM: Double? = null

    fun onFrame(frame: TelemetryFrame, savedFuelData: SavedFuelData? = null): FuelEstimate? {
        val fuelNow = frame.car?.fuel?.fuelLiters?.toDouble()
            ?.takeIf { it.isValidFuelLevel() }
            ?: return null

        val speedKmh = (frame.car?.speedKmh?.toDouble() ?: 0.0).coerceAtLeast(0.0)
        val timestampNs = frame.timestampNs

        updateSessionCache(frame)

        val gameFuelPerLap = frame.car?.fuel?.fuelPerLapLiters
        val gameFuelEstimatedLaps = frame.car?.fuel?.fuelEstimatedLaps
        val maxFuel = frame.car?.fuel?.maxFuelLiters?.toDouble()

        val dtSecRaw = computeDeltaTimeSec(timestampNs)
        if (dtSecRaw == null) {
            initializeFuelState(fuelNow)
            return buildEstimate(frame, savedFuelData, fuelNow, maxFuel, gameFuelPerLap, gameFuelEstimatedLaps)
        }

        if (dtSecRaw > tuning.dtMaxSec) {
            resetPredictiveState(fuelNow)
            return buildEstimate(frame, savedFuelData, fuelNow, maxFuel, gameFuelPerLap, gameFuelEstimatedLaps)
        }

        val prevFuel = lastFuelLiters ?: fuelNow
        lastFuelLiters = fuelNow

        if (fuelNow - prevFuel > tuning.refuelThresholdLiters) {
            handleRefuel(fuelNow, timestampNs)
            return buildEstimate(frame, savedFuelData, fuelNow, maxFuel, gameFuelPerLap, gameFuelEstimatedLaps)
        }

        val fuelConsumed = max(0.0, prevFuel - fuelNow)
        val isMoving = speedKmh >= tuning.movingMinSpeedKmh
        val dtSecForEwma = min(dtSecRaw, tuning.dtMaxSec)
        val hasGamePerLapData = gameFuelPerLap != null && gameFuelPerLap > 0

        if (isMoving) {
            updateMovingMetrics(speedKmh, fuelConsumed, dtSecRaw, dtSecForEwma)
        }

        val shouldIntegratePredictive = isMoving && !hasGamePerLapData &&
            (phase == FuelPhase.WARMUP || phase == FuelPhase.PREDICTIVE)

        if (shouldIntegratePredictive) {
            movingTimeSec += dtSecRaw
            movingFuelBurnedLiters += fuelConsumed
        }

        updateLapTimeFromSpeed(dtSecForEwma)
        updatePhase(speedKmh, timestampNs, hasGamePerLapData)

        return buildEstimate(frame, savedFuelData, fuelNow, maxFuel, gameFuelPerLap, gameFuelEstimatedLaps)
    }

    fun reset() {
        phase = FuelPhase.PIT_WAITING
        hasExitedPits = false
        lastTimestampNs = 0L
        warmupStartNs = 0L
        lastFuelLiters = null

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

    private fun updateSessionCache(frame: TelemetryFrame) {
        frame.session?.car?.carModel?.let { cachedCarModel = it }
        frame.session?.track?.trackId?.let { cachedTrackId = it }
        frame.session?.track?.lengthMeters
            ?.toDouble()
            ?.takeIf { it > 0.0 }
            ?.let { cachedTrackLengthM = it }
    }

    private fun initializeFuelState(fuelNow: Double) {
        lastFuelLiters = fuelNow
        secondsSinceFuelTick = 0.0
    }

    private fun resetPredictiveState(fuelNow: Double) {
        lastFuelLiters = fuelNow
        secondsSinceFuelTick = 0.0
        movingDistanceM = 0.0
        movingTimeSec = 0.0
        movingFuelBurnedLiters = 0.0

        fuelRateLpsEwma.reset()
        speedKmhEwma.reset()
        lapTimeSecFromSpeedEwma.reset()
    }

    private fun Double.isValidFuelLevel(): Boolean =
        isFinite() && this in tuning.minPlausibleFuelLiters..tuning.maxPlausibleFuelLiters

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
        fuelRateLpsEwma.reset()
        secondsSinceFuelTick = 0.0
        movingDistanceM = 0.0
        movingTimeSec = 0.0
        movingFuelBurnedLiters = 0.0
        lapTimeSecFromSpeedEwma.reset()

        if (phase != FuelPhase.PIT_WAITING) {
            phase = FuelPhase.WARMUP
            warmupStartNs = nowNs
        }
    }

    private fun updateMovingMetrics(speedKmh: Double, fuelConsumed: Double, dtSecRaw: Double, dtSecForEwma: Double) {
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

    private fun updatePhase(speedKmh: Double, nowNs: Long, hasGamePerLapData: Boolean) {
        when (phase) {
            FuelPhase.PIT_WAITING -> {
                if (!hasExitedPits && speedKmh >= tuning.pitExitSpeedKmh) {
                    hasExitedPits = true
                    phase = FuelPhase.WARMUP
                    warmupStartNs = nowNs
                    resetPredictiveAccumulators()
                }
            }

            FuelPhase.WARMUP -> {
                if (hasGamePerLapData) {
                    phase = FuelPhase.PER_LAP
                    return
                }
                val elapsedSec = (nowNs - warmupStartNs).toDouble() * EPS_9_DOUBLE
                if (elapsedSec >= tuning.warmupDurationSec) {
                    phase = FuelPhase.PREDICTIVE
                }
            }

            FuelPhase.PREDICTIVE -> {
                if (hasGamePerLapData) {
                    phase = FuelPhase.PER_LAP
                }
            }

            FuelPhase.PER_LAP -> Unit
        }
    }

    private fun resetPredictiveAccumulators() {
        movingDistanceM = 0.0
        movingTimeSec = 0.0
        movingFuelBurnedLiters = 0.0
        secondsSinceFuelTick = 0.0
    }

    private fun updateLapTimeFromSpeed(dtSec: Double) {
        val trackLenM = cachedTrackLengthM ?: return
        if (trackLenM < tuning.minTrackLengthMeters) return

        val avgSpeedKmh = speedKmhEwma.value
        if (avgSpeedKmh < tuning.movingMinSpeedKmh) return

        val speedMps = (avgSpeedKmh / KMH_PER_MPS).coerceAtLeast(MIN_SPEED_MPS_FOR_LAPTIME)
        val lapTimeSec = (trackLenM / speedMps).coerceIn(tuning.minLapTimeSec, tuning.maxLapTimeSec)
        lapTimeSecFromSpeedEwma.update(dtSec, lapTimeSec)
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
        val litersPerLap = estimateLitersPerLap(savedFuelData, gameFuelPerLap, lapTimeSec)
        val litersPerSecond = fuelRateLpsEwma.value.takeIf { it > 0.0 }

        val lapsRemaining = calculateLapsRemaining(
            currentFuel,
            litersPerLap,
            gameFuelEstimatedLaps,
        )

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
            completedLaps = frame.lap?.completedLaps ?: frame.session?.completedLaps ?: 0,
            confidence = estimateConfidence(gameFuelPerLap),
            isCurrentLapValid = frame.lap?.validity.isValid(),
        )
    }

    private fun LapValidity?.isValid(): Boolean =
        this == null || this == LapValidity.VALID || this == LapValidity.UNKNOWN

    private fun calculateLapsRemaining(
        currentFuel: Double,
        litersPerLap: Double?,
        gameFuelEstimatedLaps: Float?,
    ): Double? = when {
        phase == FuelPhase.PER_LAP && gameFuelEstimatedLaps != null && gameFuelEstimatedLaps > 0 ->
            gameFuelEstimatedLaps.toDouble()

        litersPerLap != null && litersPerLap > 0 -> currentFuel / litersPerLap

        else -> null
    }

    private fun estimateLapTimeSec(frame: TelemetryFrame, savedFuelData: SavedFuelData?): Pair<Double?, Boolean> {
        frame.lap?.lastLapTimeMs
            ?.takeIf { it > 0 }
            ?.let { ms ->
                val sec = ms / MS_PER_SECOND
                if (sec in tuning.minLapTimeSec..tuning.maxLapTimeSec) {
                    return sec to true
                }
            }

        lapTimeSecFromSpeedEwma.value
            .takeIf { it > 0.0 }
            ?.let { return it to false }

        savedFuelData?.bestValidLapTimeMs
            ?.takeIf { it > 0 }
            ?.let { ms ->
                val sec = ms / MS_PER_SECOND
                if (sec in tuning.minLapTimeSec..tuning.maxLapTimeSec) {
                    return sec to false
                }
            }

        return tuning.fallbackLapTimeSec to false
    }

    private fun estimateLitersPerLap(
        savedFuelData: SavedFuelData?,
        gameFuelPerLap: Float?,
        lapTimeSec: Double?,
    ): Double? {
        if (phase == FuelPhase.PER_LAP) {
            gameFuelPerLap?.toDouble()
                ?.takeIf { it in MIN_PLAUSIBLE_LITERS_PER_LAP..tuning.maxPlausibleLitersPerLap }
                ?.let { return it }
        }

        if (gameFuelPerLap == null || gameFuelPerLap <= 0) {
            savedFuelData?.peakLitersPerLap
                ?.takeIf { it.isFinite() && it in MIN_PLAUSIBLE_LITERS_PER_LAP..tuning.maxPlausibleLitersPerLap }
                ?.let { return it }
        }

        estimatePredictiveFromDistance()?.let { return it }

        estimatePredictiveFromRate(lapTimeSec ?: 0.0, savedFuelData)?.let { return it }

        return gameFuelPerLap?.toDouble()
            ?.takeIf { it in MIN_PLAUSIBLE_LITERS_PER_LAP..tuning.maxPlausibleLitersPerLap }
    }

    private fun estimatePredictiveFromDistance(): Double? {
        val trackLenM = cachedTrackLengthM ?: return null
        if (trackLenM < tuning.minTrackLengthMeters) return null

        val minDistM = if (phase == FuelPhase.WARMUP) MIN_DISTANCE_WARMUP_M else MIN_DISTANCE_PREDICTIVE_M
        if (movingDistanceM < minDistM) return null
        if (movingFuelBurnedLiters < tuning.minFuelBurnedForEstimate) return null

        val litersPerLap = (movingFuelBurnedLiters / movingDistanceM) * trackLenM
        return litersPerLap.takeIf { it in MIN_PLAUSIBLE_LITERS_PER_LAP..tuning.maxPlausibleLitersPerLap }
    }

    private fun estimatePredictiveFromRate(lapTimeSec: Double, savedFuelData: SavedFuelData?): Double? {
        if (lapTimeSec <= 0.0) return null

        val hasEnoughFuelSignal =
            movingFuelBurnedLiters >= tuning.minFuelBurnedForEstimate || fuelRateLpsEwma.value > 0.0
        if (!hasEnoughFuelSignal) return null

        val avgRateLps = if (movingTimeSec > 0.0) movingFuelBurnedLiters / movingTimeSec else 0.0
        val ewmaRateLps = fuelRateLpsEwma.value

        val blendedRate = when {
            avgRateLps > 0.0 && ewmaRateLps > 0.0 ->
                BLEND_RATE_WEIGHT * avgRateLps +
                    (1.0 - BLEND_RATE_WEIGHT) * ewmaRateLps

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

    private fun estimateConfidence(gameFuelPerLap: Float?): Double = when (phase) {
        FuelPhase.PIT_WAITING -> 0.0

        FuelPhase.WARMUP -> {
            val elapsedSec = (lastTimestampNs - warmupStartNs).toDouble() * EPS_9_DOUBLE
            val progress = (elapsedSec / tuning.warmupDurationSec).coerceIn(0.0, 1.0)
            progress * tuning.predictiveConfMin
        }

        FuelPhase.PREDICTIVE -> {
            val distanceFactor = min(1.0, movingDistanceM / CONF_DISTANCE_SCALE_M)
            tuning.predictiveConfMin + (tuning.predictiveConfMax - tuning.predictiveConfMin) * distanceFactor
        }

        FuelPhase.PER_LAP -> {
            if (gameFuelPerLap != null && gameFuelPerLap > 0) {
                tuning.perLapConf2Plus
            } else {
                tuning.predictiveConfMax
            }
        }
    }

    private companion object {
        const val KMH_PER_MPS = 3.6
        const val MS_PER_SECOND = 1_000.0
        const val MIN_POSITIVE_WINDOW_SEC = 1e-6
        const val MIN_SPEED_MPS_FOR_LAPTIME = 0.1
        const val MIN_PLAUSIBLE_LITERS_PER_LAP = 0.1
        const val MIN_DISTANCE_WARMUP_M = 200.0
        const val MIN_DISTANCE_PREDICTIVE_M = 800.0
        const val BLEND_RATE_WEIGHT = 0.5
        const val SAVED_BASELINE_CAP_MULTIPLIER = 1.40
        const val CONF_DISTANCE_SCALE_M = 10_000.0
    }
}
