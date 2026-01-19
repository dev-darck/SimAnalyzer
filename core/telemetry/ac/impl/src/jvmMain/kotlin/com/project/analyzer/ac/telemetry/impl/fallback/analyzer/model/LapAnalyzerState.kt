package com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model

import com.project.analyzer.ac.telemetry.impl.fallback.pose.model.CarPose
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

class LapAnalyzerState {

    var isActive = false
    var currentTrackId: String? = null
    var calibration: TrackCalibration? = null
    var referencePoint: ReferencePoint = ReferencePoint.FRONT_AXLE

    var previousPose: CarPose? = null
    var previousTimestampNs: Long = 0L

    var isLapRunning = false
    var isSyncedToStartFinish = false

    var unsyncedLapStartTimeNs = 0L

    var lapStartTimeNs = 0L
    var sectorStartTimeNs = 0L
    var currentSectorIndex = 1
    var completedLapsCount = 0

    var lastLapTimeMs: Int? = null
    var bestLapTimeMs: Int? = null
    var lastSectorTimeMs: Int? = null

    var lastSector1Ms: Int? = null
    var lastSector2Ms: Int? = null
    var lastSector3Ms: Int? = null
    var bestSector1Ms: Int? = null
    var bestSector2Ms: Int? = null
    var bestSector3Ms: Int? = null

    private val gateLastTriggerNs = mutableMapOf<String, Long>()
    private val gateCooldownNs = 900_000_000L

    var currentLapValid: Boolean = true
        private set
    var lastLapValid: Boolean = true
        private set
    var bestValidLapTimeMs: Int? = null
        private set
    var startFinishSyncId: Int = 0
        private set

    private var maxDirtyLevelThisLap: Float = 0f
    private var currentDamage: Float = 0f
    private var hadPenaltyThisLap: Boolean = false
    private var hadOffTrackThisLap: Boolean = false
    private var hasDamage: Boolean = false

    fun reset() {
        isActive = false
        currentTrackId = null
        calibration = null
        referencePoint = ReferencePoint.FRONT_AXLE
        resetSession()

        currentLapValid = true
        lastLapValid = true
        bestValidLapTimeMs = null
        maxDirtyLevelThisLap = 0f
        currentDamage = 0f
        hadPenaltyThisLap = false
        hadOffTrackThisLap = false
        hasDamage = false
    }

    fun resetSession() {
        previousPose = null
        previousTimestampNs = 0L
        isLapRunning = false
        isSyncedToStartFinish = false
        unsyncedLapStartTimeNs = 0L
        lapStartTimeNs = 0L
        sectorStartTimeNs = 0L
        currentSectorIndex = 1
        completedLapsCount = 0
        lastLapTimeMs = null
        bestLapTimeMs = null
        lastSectorTimeMs = null
        lastSector1Ms = null
        lastSector2Ms = null
        lastSector3Ms = null
        bestSector1Ms = null
        bestSector2Ms = null
        bestSector3Ms = null
        gateLastTriggerNs.clear()
        isActive = true
        startFinishSyncId = 0

        currentLapValid = true
        lastLapValid = true
        maxDirtyLevelThisLap = 0f
        currentDamage = 0f
        hasDamage = false
        hadPenaltyThisLap = false
        hadOffTrackThisLap = false
    }

    fun updateValidity(
        tyreDirtyLevel: FloatArray?,
        carDamage: FloatArray?,
        numberOfTyresOut: Int,
        hasPenalty: Boolean
    ) {
        tyreDirtyLevel?.let { dirty ->
            if (dirty.size >= 4) {
                val maxDirty = dirty.take(4).maxOrNull() ?: 0f
                if (maxDirty > maxDirtyLevelThisLap) {
                    maxDirtyLevelThisLap = maxDirty
                }

                val allDirty = dirty.take(4).all { it > DIRTY_THRESHOLD }
                if (allDirty) {
                    hadOffTrackThisLap = true
                }
            }
        }

        carDamage?.let { damage ->
            val maxDmg = damage.maxOrNull() ?: 0f
            if (maxDmg > currentDamage) {
                currentDamage = maxDmg
                hasDamage = currentDamage > DAMAGE_THRESHOLD
            }
            if (maxDmg < currentDamage) currentDamage = maxDmg
        }

        if (numberOfTyresOut >= 4) {
            hadOffTrackThisLap = true
        }

        if (hasPenalty) {
            hadPenaltyThisLap = true
        }

        currentLapValid = !hadPenaltyThisLap &&
            !hadOffTrackThisLap &&
            !hasDamage
    }

    fun invalidateCurrentLap() {
        hadPenaltyThisLap = true
        currentLapValid = false
    }

    fun resetWithTrackId(trackId: String) {
        reset()
        isActive = true
        currentTrackId = trackId
    }

    fun setCalibration(trackId: String, cal: TrackCalibration) {
        currentTrackId = trackId
        calibration = cal
        referencePoint = cal.referencePoint
        isActive = true
        resetSession()
    }

    fun markActive() {
        isActive = true
    }

    fun ensureLapStarted(timestampNs: Long) {
        if (!isLapRunning) {
            isLapRunning = true
            isSyncedToStartFinish = false
            unsyncedLapStartTimeNs = timestampNs
            lapStartTimeNs = timestampNs
            sectorStartTimeNs = timestampNs
            currentSectorIndex = 1
            lastSectorTimeMs = null
        }
        if (previousTimestampNs == 0L) previousTimestampNs = timestampNs
    }

    fun updateFrame(timestampNs: Long) {
        previousTimestampNs = timestampNs
    }

    fun syncToStartFinish(timestampNs: Long, interpolationFactor: Float) {
        startFinishSyncId += 1
        isSyncedToStartFinish = true
        completedLapsCount = 0
        lastLapTimeMs = null
        bestLapTimeMs = null
        lastSectorTimeMs = null
        lastSector1Ms = null
        lastSector2Ms = null
        lastSector3Ms = null

        val crossingTimeNs = interpolateTimestamp(timestampNs, interpolationFactor)
        lapStartTimeNs = crossingTimeNs
        sectorStartTimeNs = crossingTimeNs
        currentSectorIndex = 1

        resetLapValidityTracking()
    }

    fun completeSector(timestampNs: Long, interpolationFactor: Float) {
        val crossingTimeNs = interpolateTimestamp(timestampNs, interpolationFactor)
        val sectorTimeMs = ((crossingTimeNs - sectorStartTimeNs) / NS_PER_MS).toInt()

        lastSectorTimeMs = sectorTimeMs
        updateSectorBest(currentSectorIndex, sectorTimeMs)

        sectorStartTimeNs = crossingTimeNs
        currentSectorIndex += 1
    }

    fun completeLap(timestampNs: Long, interpolationFactor: Float) {
        val crossingTimeNs = interpolateTimestamp(timestampNs, interpolationFactor)

        val sector3TimeMs = ((crossingTimeNs - sectorStartTimeNs) / NS_PER_MS).toInt()
        lastSectorTimeMs = sector3TimeMs
        updateSectorBest(3, sector3TimeMs)

        val lapTimeMs = ((crossingTimeNs - lapStartTimeNs) / NS_PER_MS).toInt()
        lastLapTimeMs = lapTimeMs

        lastLapValid = currentLapValid

        if (currentLapValid) {
            bestLapTimeMs = bestLapTimeMs?.let { minOf(it, lapTimeMs) } ?: lapTimeMs
            bestValidLapTimeMs = bestValidLapTimeMs?.let { minOf(it, lapTimeMs) } ?: lapTimeMs
        } else {
            bestLapTimeMs = bestLapTimeMs?.let { minOf(it, lapTimeMs) } ?: lapTimeMs
        }

        completedLapsCount += 1

        lapStartTimeNs = crossingTimeNs
        sectorStartTimeNs = crossingTimeNs
        currentSectorIndex = 1

        resetLapValidityTracking()
    }

    fun canTriggerGate(timestampNs: Long, gateKey: String): Boolean {
        val lastTrigger = gateLastTriggerNs[gateKey] ?: return true
        return (timestampNs - lastTrigger) >= gateCooldownNs
    }

    fun markGateTriggered(timestampNs: Long, gateKey: String) {
        gateLastTriggerNs[gateKey] = timestampNs
    }

    fun createSnapshot(currentTimeNs: Long): LapTimingSnapshot {
        val currentLapMs = if (isLapRunning) {
            val startTime = if (isSyncedToStartFinish) lapStartTimeNs else unsyncedLapStartTimeNs
            ((currentTimeNs - startTime) / NS_PER_MS).toInt()
        } else {
            0
        }

        val currentSectorMs = if (isLapRunning) {
            val startTime = if (isSyncedToStartFinish) sectorStartTimeNs else unsyncedLapStartTimeNs
            ((currentTimeNs - startTime) / NS_PER_MS).toInt()
        } else {
            0
        }

        val sectorIndex = if (isLapRunning && isSyncedToStartFinish) {
            (currentSectorIndex - 1).coerceIn(0, 2)
        } else {
            0
        }

        val (deltaMs, isPositive) = calculateDelta(currentLapMs)

        return LapTimingSnapshot(
            isActive = isActive,
            trackId = currentTrackId,
            isLapRunning = isLapRunning,
            completedLapsCount = completedLapsCount,
            currentLapTimeMs = currentLapMs,
            currentSectorTimeMs = currentSectorMs,
            currentSectorIndex = sectorIndex,
            lastSectorTimeMs = lastSectorTimeMs,
            lastLapTimeMs = lastLapTimeMs,
            bestLapTimeMs = bestLapTimeMs,
            lastSector1Ms = lastSector1Ms,
            lastSector2Ms = lastSector2Ms,
            lastSector3Ms = lastSector3Ms,
            bestSector1Ms = bestSector1Ms,
            bestSector2Ms = bestSector2Ms,
            bestSector3Ms = bestSector3Ms,
            currentLapValid = currentLapValid,
            lastLapValid = lastLapValid,
            bestValidLapTimeMs = bestValidLapTimeMs,
            deltaLapTimeMs = deltaMs,
            isDeltaPositive = isPositive,
            startFinishSyncId = startFinishSyncId
        )
    }

    private fun calculateDelta(currentLapMs: Int): Pair<Int?, Boolean> {
        val best = bestLapTimeMs ?: return null to true
        if (currentLapMs <= 0 || best <= 0) return null to true

        val delta = currentLapMs - best
        return delta to (delta >= 0)
    }

    private fun resetLapValidityTracking() {
        currentLapValid = true
        maxDirtyLevelThisLap = 0f
        hadPenaltyThisLap = false
        hadOffTrackThisLap = false
        hasDamage = false
    }

    private fun updateSectorBest(sectorNumber: Int, timeMs: Int) {
        when (sectorNumber) {
            1 -> {
                lastSector1Ms = timeMs
                bestSector1Ms = bestSector1Ms?.let { minOf(it, timeMs) } ?: timeMs
            }

            2 -> {
                lastSector2Ms = timeMs
                bestSector2Ms = bestSector2Ms?.let { minOf(it, timeMs) } ?: timeMs
            }

            3 -> {
                lastSector3Ms = timeMs
                bestSector3Ms = bestSector3Ms?.let { minOf(it, timeMs) } ?: timeMs
            }
        }
    }

    private fun interpolateTimestamp(currentNs: Long, alpha: Float): Long {
        val dt = if (previousTimestampNs > 0) (currentNs - previousTimestampNs) else 16_000_000L
        return currentNs - ((1f - alpha) * dt).toLong()
    }

    private companion object {

        const val NS_PER_MS = 1_000_000L
        const val DIRTY_THRESHOLD = 0.2f
        const val DAMAGE_THRESHOLD = 0.5f
    }
}
