package com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model

import com.project.analyzer.ac.telemetry.impl.fallback.pose.model.CarPose
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

    private var lastSectorsMs: IntArray = IntArray(0)
    private var bestSectorsMs: IntArray = IntArray(0)
    private val gateLastTriggerNs = mutableMapOf<String, Long>()
    private val gateCooldownNs = 900.milliseconds.inWholeNanoseconds

    var currentLapValid: Boolean = true
        private set
    var startFinishSyncId: Int = 0
        private set

    private var maxDirtyLevelThisLap: Float = 0f
    private var baselineDamage: Float = 0f
    private var currentDamage: Float = 0f
    private var hadPenaltyThisLap: Boolean = false
    private var hadOffTrackThisLap: Boolean = false
    private var hadNewDamageThisLap: Boolean = false

    val sectorCount: Int
        get() = ((calibration?.sectors?.size ?: DEFAULT_INTERMEDIATE_GATES)).coerceAtLeast(1)

    fun reset() {
        isActive = false
        currentTrackId = null
        calibration = null
        referencePoint = ReferencePoint.FRONT_AXLE
        resetSession()

        currentLapValid = true
        maxDirtyLevelThisLap = 0f
        baselineDamage = 0f
        currentDamage = 0f
        hadPenaltyThisLap = false
        hadOffTrackThisLap = false
        hadNewDamageThisLap = false
    }

    fun rebasePose(resumeTimeNs: Long) {
        previousPose = null
        previousTimestampNs = resumeTimeNs
    }

    fun softResetAfterRespawn(nowNs: Long) {
        previousPose = null
        previousTimestampNs = nowNs

        isLapRunning = false
        isSyncedToStartFinish = false

        unsyncedLapStartTimeNs = 0L
        lapStartTimeNs = 0L
        sectorStartTimeNs = 0L

        currentSectorIndex = 1
        lastSectorTimeMs = null

        gateLastTriggerNs.clear()
        resetLapValidityTracking()
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

        resizeSectorArrays(sectorCount)
        clearLastAndBestSectors()

        gateLastTriggerNs.clear()
        isActive = true
        startFinishSyncId = 0

        currentLapValid = true

        maxDirtyLevelThisLap = 0f
        baselineDamage = 0f
        currentDamage = 0f
        hadPenaltyThisLap = false
        hadOffTrackThisLap = false
        hadNewDamageThisLap = false
    }

    fun updateValidity(
        tyreDirtyLevel: FloatArray?,
        carDamage: FloatArray?,
        numberOfTyresOut: Int,
        hasPenalty: Boolean,
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
            currentDamage = maxDmg

            val newDamageThisLap = maxDmg - baselineDamage
            if (newDamageThisLap > DAMAGE_DELTA_THRESHOLD) {
                hadNewDamageThisLap = true
            }
        }

        if (numberOfTyresOut >= 4) {
            hadOffTrackThisLap = true
        }

        if (hasPenalty) {
            hadPenaltyThisLap = true
        }

        currentLapValid = !hadPenaltyThisLap &&
            !hadOffTrackThisLap &&
            !hadNewDamageThisLap
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
        clearLastAndBestSectors()

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

        val finalSectorTimeMs = ((crossingTimeNs - sectorStartTimeNs) / NS_PER_MS).toInt()
        lastSectorTimeMs = finalSectorTimeMs
        updateSectorBest(sectorCount, finalSectorTimeMs)

        val lapTimeMs = ((crossingTimeNs - lapStartTimeNs) / NS_PER_MS).toInt()
        lastLapTimeMs = lapTimeMs

        if (currentLapValid) {
            bestLapTimeMs = bestLapTimeMs?.let { min(it, lapTimeMs) } ?: lapTimeMs
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

        val sectorIndex0Based = if (isLapRunning && isSyncedToStartFinish) {
            (currentSectorIndex - 1).coerceIn(0, sectorCount - 1)
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
            currentSectorIndex = sectorIndex0Based,
            lastSectorTimeMs = lastSectorTimeMs,
            lastLapTimeMs = lastLapTimeMs,
            bestLapTimeMs = bestLapTimeMs,
            lastSectorsMs = lastSectorsMs.toNullableList(),
            bestSectorsMs = bestSectorsMs.toNullableList(),
            currentLapValid = currentLapValid,
            deltaLapTimeMs = deltaMs,
            isDeltaPositive = isPositive,
            startFinishSyncId = startFinishSyncId,
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
        baselineDamage = currentDamage
        hadPenaltyThisLap = false
        hadOffTrackThisLap = false
        hadNewDamageThisLap = false
    }

    private fun clearLastAndBestSectors() {
        lastSectorsMs.fill(0)
        bestSectorsMs.fill(0)
    }

    private fun updateSectorBest(sectorNumber: Int, timeMs: Int) {
        if (sectorNumber <= 0 || timeMs <= 0) return
        ensureSectorArrays(sectorCount)
        val idx = sectorNumber - 1
        if (idx !in lastSectorsMs.indices) return

        lastSectorsMs[idx] = timeMs

        if (currentLapValid) {
            val prevBest = bestSectorsMs[idx]
            bestSectorsMs[idx] = when {
                prevBest <= 0 -> timeMs
                else -> min(prevBest, timeMs)
            }
        }
    }

    private fun ensureSectorArrays(requiredSize: Int) {
        if (requiredSize <= 0) return
        if (lastSectorsMs.size != requiredSize || bestSectorsMs.size != requiredSize) {
            resizeSectorArrays(requiredSize)
        }
    }

    private fun resizeSectorArrays(newSize: Int) {
        val size = newSize.coerceAtLeast(1)
        val newLast = IntArray(size)
        val newBest = IntArray(size)

        if (lastSectorsMs.isNotEmpty()) {
            lastSectorsMs.copyInto(newLast, endIndex = min(lastSectorsMs.size, newLast.size))
        }
        if (bestSectorsMs.isNotEmpty()) {
            bestSectorsMs.copyInto(newBest, endIndex = min(bestSectorsMs.size, newBest.size))
        }

        lastSectorsMs = newLast
        bestSectorsMs = newBest
    }

    private fun IntArray.toNullableList(): List<Int?> = this.map { it.takeIf { v -> v > 0 } }

    private fun interpolateTimestamp(currentNs: Long, alpha: Float): Long {
        if (previousTimestampNs <= 0L) return currentNs
        val dt = currentNs - previousTimestampNs
        if (dt !in 1..MAX_REASONABLE_FRAME_NS) return currentNs
        val offset = (1.0 - alpha.toDouble()) * dt.toDouble()
        return currentNs - offset.toLong()
    }

    private companion object {
        val NS_PER_MS = 1.milliseconds.inWholeNanoseconds
        val MAX_REASONABLE_FRAME_NS = 1.seconds.inWholeNanoseconds
        const val DIRTY_THRESHOLD = 0.2f
        const val DAMAGE_DELTA_THRESHOLD = 0.1f

        const val DEFAULT_INTERMEDIATE_GATES = 2
    }
}
