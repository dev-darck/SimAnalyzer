package com.project.analyzer.ac.telemetry.impl.fallback.analyzer

import com.project.analyzer.ac.telemetry.impl.fallback.TrackCalibrationLoader
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapAnalyzerState
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.detector.GateCrossingDetector
import com.project.analyzer.ac.telemetry.impl.fallback.pose.PhysicsPoseExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.pose.model.CarPose
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.api.di.IO
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(AppScope::class)
class FallbackLapAnalyzer(
    private val calibrationLoader: TrackCalibrationLoader,
    private val gateDetector: GateCrossingDetector,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val state = LapAnalyzerState()
    private val poseExtractor = PhysicsPoseExtractor()
    private val calibrationLoadScope = CoroutineScope(
        SupervisorJob() + ioDispatcher.limitedParallelism(1, "FallbackLapAnalyzerCalibration"),
    )
    private val requestedCalibrationLoads = ConcurrentHashMap.newKeySet<String>()

    private var lastMissingCalibrationTrackKey: String? = null
    private var lastMissingCalibrationLogMs: Long = 0L
    private var lastProcessedTimestampNs: Long = -1L

    fun loadCalibration(trackId: String, calibration: TrackCalibration) {
        state.setCalibration(trackId, calibration)
    }

    fun processPose(timestampNs: Long, pose: CarPose, calibration: TrackCalibration) {
        if (!state.isActive) return
        if (state.currentTrackId != calibration.trackId) return

        state.ensureLapStarted(timestampNs)

        val previous = state.previousPose
        if (previous == null) {
            state.previousPose = pose
            return
        }

        processFrame(
            timestampNs = timestampNs,
            previousPose = previous,
            currentPose = pose,
            calibration = calibration,
            sectorIndexHint0Based = null,
            lastSectorTimeHintMs = null,
        )
    }

    fun getSnapshot(currentTimeNs: Long): LapTimingSnapshot = state.createSnapshot(currentTimeNs)

    fun isSyncedToStartFinish(): Boolean = state.isSyncedToStartFinish

    fun getStartFinishSyncId(): Int = state.startFinishSyncId

    fun getCompletedLapsCount(): Int = state.completedLapsCount

    fun rebasePose(resumeTimeNs: Long) = state.rebasePose(resumeTimeNs)

    fun softResetAfterRespawn(nowNs: Long) {
        state.softResetAfterRespawn(nowNs)
        lastProcessedTimestampNs = -1L
    }

    fun loadCalibration(trackId: String?): TrackCalibration? {
        val id = trackId?.takeIf { it.isNotBlank() } ?: return run {
            state.markActive()
            null
        }

        val cachedCalibration = calibrationLoader.peek(id)
        if (cachedCalibration != null) {
            if (lastMissingCalibrationTrackKey == normalizeTrackLogKey(id)) {
                lastMissingCalibrationTrackKey = null
            }
            requestedCalibrationLoads.remove(id)
            if (state.currentTrackId != id || state.calibration != cachedCalibration) {
                state.setCalibration(id, cachedCalibration)
            }
            return cachedCalibration
        }

        if (!calibrationLoader.isCached(id)) {
            requestCalibrationLoad(id)
        } else {
            maybeLogMissingCalibration(id)
        }

        if (state.currentTrackId != id || state.calibration != null) {
            state.resetWithTrackId(id)
        }
        return null
    }

    fun processPhysicsFrame(
        timestampNs: Long,
        physics: SPageFilePhysics,
        sectorIndexHint0Based: Int? = null,
        lastSectorTimeHintMs: Int? = null,
    ) {
        val calibration = state.calibration ?: return
        if (timestampNs == lastProcessedTimestampNs) return
        val pose = poseExtractor.extract(physics, state.referencePoint) ?: return state.markActive()
        lastProcessedTimestampNs = timestampNs
        val sanitizedSectorHint = sanitizeGameSectorHint(sectorIndexHint0Based, calibration)

        state.ensureLapStarted(timestampNs)

        state.updateValidity(
            tyreDirtyLevel = physics.tyreDirtyLevel,
            carDamage = physics.carDamage,
            numberOfTyresOut = physics.numberOfTyresOut,
            hasPenalty = false,
        )

        val previous = state.previousPose
        if (previous == null) {
            state.lastObservedGameSectorIndex0Based = sanitizedSectorHint
            state.previousPose = pose
            return
        }

        processFrame(
            timestampNs = timestampNs,
            previousPose = previous,
            currentPose = pose,
            calibration = calibration,
            sectorIndexHint0Based = sanitizedSectorHint,
            lastSectorTimeHintMs = lastSectorTimeHintMs,
        )
    }

    fun onPenaltyDetected() {
        state.invalidateCurrentLap()
    }

    fun resetSession() {
        state.resetSession()
        lastProcessedTimestampNs = -1L
    }

    fun reset() {
        state.reset()
        lastProcessedTimestampNs = -1L
        requestedCalibrationLoads.clear()
    }

    private fun requestCalibrationLoad(trackId: String) {
        if (!requestedCalibrationLoads.add(trackId)) return
        calibrationLoadScope.launch {
            runCatching {
                calibrationLoader.load(trackId = trackId)
            }.onFailure { error ->
                logger.warn(error) { "TrackCalibration load failed for trackId=$trackId" }
            }
            requestedCalibrationLoads.remove(trackId)
        }
    }

    private fun maybeLogMissingCalibration(trackId: String) {
        val trackKey = normalizeTrackLogKey(trackId)
        val now = System.currentTimeMillis()
        val shouldLog =
            lastMissingCalibrationTrackKey != trackKey ||
                (now - lastMissingCalibrationLogMs) >= MISSING_CALIBRATION_LOG_COOLDOWN_MS

        if (!shouldLog) return

        logger.info {
            "TrackCalibration NOT found for trackId=$trackId. " +
                "Checked app calibration folder for $trackId.json"
        }
        lastMissingCalibrationTrackKey = trackKey
        lastMissingCalibrationLogMs = now
    }

    private fun normalizeTrackLogKey(trackId: String): String = trackId
        .trim()
        .lowercase()
        .replace("_", "")

    private fun processFrame(
        timestampNs: Long,
        previousPose: CarPose,
        currentPose: CarPose,
        calibration: TrackCalibration,
        sectorIndexHint0Based: Int?,
        lastSectorTimeHintMs: Int?,
    ) {
        state.ensureLapStarted(timestampNs)

        val stationary = isCarStationary(currentPose, previousPose)
        if (stationary) {
            state.lastObservedGameSectorIndex0Based = sectorIndexHint0Based
            state.previousPose = currentPose
            state.updateFrame(timestampNs)
            return
        }

        if (!state.isSyncedToStartFinish) {
            trySyncToStartFinish(timestampNs, previousPose, currentPose, calibration)
            state.lastObservedGameSectorIndex0Based = sectorIndexHint0Based
            state.previousPose = currentPose
            state.updateFrame(timestampNs)
            return
        }

        applyGameSectorProgressHint(
            timestampNs = timestampNs,
            sectorIndexHint0Based = sectorIndexHint0Based,
            lastSectorTimeHintMs = lastSectorTimeHintMs,
            calibration = calibration,
        )
        checkSectorCrossings(timestampNs, previousPose, currentPose, calibration)
        checkLapCompletion(timestampNs, previousPose, currentPose, calibration)

        state.previousPose = currentPose
        state.updateFrame(timestampNs)
    }

    private fun isCarStationary(current: CarPose, previous: CarPose): Boolean {
        val dp = current.position - previous.position
        val dpLen = dp.len()

        val almostNoMovement = dpLen < 0.005f

        val almostNoSpeed = current.speedKmh < 0.5f

        return almostNoMovement && almostNoSpeed
    }

    private fun trySyncToStartFinish(
        timestampNs: Long,
        previousPose: CarPose,
        currentPose: CarPose,
        calibration: TrackCalibration,
    ) {
        val crossing = gateDetector.detectCrossing(
            previousPose,
            currentPose,
            calibration.startFinish,
        )
        if (crossing != null && state.canTriggerGate(timestampNs, GATE_START_FINISH)) {
            if (!crossing.isForwardDirection) {
                state.previousPose = currentPose
                state.updateFrame(timestampNs)
                return
            }

            logger.debug { "LAP: Start/Finish sync acquired" }
            state.syncToStartFinish(timestampNs, crossing.interpolationFactor)
            state.markGateTriggered(timestampNs, GATE_START_FINISH)
        }
    }

    private fun applyGameSectorProgressHint(
        timestampNs: Long,
        sectorIndexHint0Based: Int?,
        lastSectorTimeHintMs: Int?,
        calibration: TrackCalibration,
    ) {
        val previousObservedSector = state.lastObservedGameSectorIndex0Based
        if (sectorIndexHint0Based == null) {
            state.lastObservedGameSectorIndex0Based = null
            return
        }
        if (previousObservedSector == sectorIndexHint0Based) return

        val sectorCount = getSectorCount(calibration)
        val expectedSectorIndex0Based = (state.currentSectorIndex - 1).coerceIn(0, sectorCount - 1)
        val forwardSteps = when {
            sectorIndexHint0Based == expectedSectorIndex0Based -> 0

            sectorIndexHint0Based > expectedSectorIndex0Based -> sectorIndexHint0Based - expectedSectorIndex0Based

            expectedSectorIndex0Based == sectorCount - 1 && sectorIndexHint0Based == 0 -> 1

            else -> {
                logger.debug {
                    "LAP: Ignore non-monotonic native sector hint " +
                        "prev=$previousObservedSector expected=$expectedSectorIndex0Based actual=$sectorIndexHint0Based"
                }
                state.lastObservedGameSectorIndex0Based = sectorIndexHint0Based
                return
            }
        }
        state.lastObservedGameSectorIndex0Based = sectorIndexHint0Based
        if (forwardSteps <= 0) return

        val sectorTimeHintMs = lastSectorTimeHintMs?.takeIf { it > 0 }
        repeat(forwardSteps) { stepIndex ->
            val timeOverride = if (stepIndex == forwardSteps - 1) sectorTimeHintMs else null
            if (state.currentSectorIndex < sectorCount) {
                val gateKey = "S${state.currentSectorIndex}_F"
                if (!state.canTriggerGate(timestampNs, gateKey)) return@repeat
                state.completeSector(timestampNs, interpolationFactor = 1f, sectorTimeMsOverride = timeOverride)
                state.markGateTriggered(timestampNs, gateKey)
            } else {
                if (!state.canTriggerGate(timestampNs, GATE_START_FINISH)) return@repeat
                logger.debug { "LAP: Start/Finish derived from native sector index wrap" }
                state.completeLap(timestampNs, interpolationFactor = 1f, finalSectorTimeMsOverride = timeOverride)
                state.markGateTriggered(timestampNs, GATE_START_FINISH)
            }
        }
    }

    private fun checkSectorCrossings(
        timestampNs: Long,
        previousPose: CarPose,
        currentPose: CarPose,
        calibration: TrackCalibration,
    ) {
        val currentSector = state.currentSectorIndex
        val nextSectorGate = findNextSectorFinishGate(calibration, currentSector) ?: return
        val gateKey = "S${currentSector}_F"

        val crossing = gateDetector.detectCrossing(
            previousPose,
            currentPose,
            nextSectorGate,
        )
        if (crossing != null && crossing.isForwardDirection && state.canTriggerGate(timestampNs, gateKey)) {
            state.completeSector(timestampNs, crossing.interpolationFactor)
            state.markGateTriggered(timestampNs, gateKey)
        }
    }

    private fun checkLapCompletion(
        timestampNs: Long,
        previousPose: CarPose,
        currentPose: CarPose,
        calibration: TrackCalibration,
    ) {
        val crossing = gateDetector.detectCrossing(previousPose, currentPose, calibration.startFinish)

        if (crossing != null && crossing.isForwardDirection && state.canTriggerGate(timestampNs, GATE_START_FINISH)) {
            if (state.isLapRunning && state.isSyncedToStartFinish) {
                val expectedFinalSector = getSectorCount(calibration)
                val isValidSequence = state.currentSectorIndex == expectedFinalSector

                if (isValidSequence) {
                    state.completeLap(timestampNs, crossing.interpolationFactor)
                } else {
                    logger.debug {
                        "LAP: Start/Finish crossed in sector ${state.currentSectorIndex}, expected $expectedFinalSector - realigning"
                    }
                    state.realignToStartFinish(timestampNs, crossing.interpolationFactor)
                }
            } else {
                state.syncToStartFinish(timestampNs, crossing.interpolationFactor)
            }
            state.markGateTriggered(timestampNs, GATE_START_FINISH)
        }
    }

    private fun sanitizeGameSectorHint(sectorIndexHint0Based: Int?, calibration: TrackCalibration): Int? {
        val sectorCount = getSectorCount(calibration)
        return sectorIndexHint0Based?.takeIf { it in 0 until sectorCount }
    }

    private fun findNextSectorFinishGate(calibration: TrackCalibration, sectorNumber: Int): Gate? {
        val totalSectors = getSectorCount(calibration)
        if (sectorNumber >= totalSectors) return null
        return calibration.sectors.firstOrNull { it.index == sectorNumber }?.finish
    }

    private fun getSectorCount(calibration: TrackCalibration): Int = calibration.sectors.size.coerceAtLeast(1)

    private companion object {

        const val GATE_START_FINISH = "SF"
        val MISSING_CALIBRATION_LOG_COOLDOWN_MS = 30.seconds.inWholeMilliseconds
    }
}
