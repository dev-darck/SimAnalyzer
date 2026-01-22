package com.project.analyzer.ac.telemetry.impl.fallback.analyzer

import com.project.analyzer.ac.telemetry.impl.fallback.TrackCalibrationLoader
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapAnalyzerState
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.detector.GateCrossingDetector
import com.project.analyzer.ac.telemetry.impl.fallback.pose.PhysicsPoseExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.pose.model.CarPose
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class FallbackLapAnalyzer(
    private val calibrationLoader: TrackCalibrationLoader,
    private val gateDetector: GateCrossingDetector
) {

    private val state = LapAnalyzerState()
    private val poseExtractor = PhysicsPoseExtractor()

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

        processFrame(timestampNs, previous, pose, calibration)
    }

    fun getSnapshot(currentTimeNs: Long): LapTimingSnapshot = state.createSnapshot(currentTimeNs)

    fun loadCalibration(trackId: String?): TrackCalibration? {
        val id = trackId?.takeIf { it.isNotBlank() } ?: return run {
            state.markActive()
            null
        }

        return if (id != state.currentTrackId) {
            val calibration = calibrationLoader.load(trackId)
            if (calibration == null) {
                state.resetWithTrackId(trackId)
                return null
            }
            state.setCalibration(trackId, calibration)
            calibration
        } else {
            state.calibration
        }
    }

    fun processPhysicsFrame(timestampNs: Long, physics: SPageFilePhysics) {
        val calibration = state.calibration ?: return
        val pose = poseExtractor.extract(physics, state.referencePoint) ?: return state.markActive()

        state.ensureLapStarted(timestampNs)

        state.updateValidity(
            tyreDirtyLevel = physics.tyreDirtyLevel,
            carDamage = physics.carDamage,
            numberOfTyresOut = physics.numberOfTyresOut,
            hasPenalty = false
        )

        val previous = state.previousPose
        if (previous == null) {
            state.previousPose = pose
            return
        }

        processFrame(timestampNs, previous, pose, calibration)
    }

    fun onPenaltyDetected() {
        state.invalidateCurrentLap()
    }

    fun resetSession() {
        state.resetSession()
    }

    fun reset() {
        state.reset()
    }

    private fun processFrame(
        timestampNs: Long,
        previousPose: CarPose,
        currentPose: CarPose,
        calibration: TrackCalibration
    ) {
        state.ensureLapStarted(timestampNs)

        val stationary = isCarStationary(currentPose, previousPose)
        if (stationary) {
            state.previousPose = currentPose
            state.updateFrame(timestampNs)
            return
        }

        if (!state.isSyncedToStartFinish) {
            trySyncToStartFinish(timestampNs, previousPose, currentPose, calibration)
            state.previousPose = currentPose
            state.updateFrame(timestampNs)
            return
        }

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
        calibration: TrackCalibration
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

            println("LAP: ✔ SYNCED to Start/Finish line")
            state.syncToStartFinish(timestampNs, crossing.interpolationFactor)
            state.markGateTriggered(timestampNs, GATE_START_FINISH)
        }
    }

    private fun checkSectorCrossings(
        timestampNs: Long,
        previousPose: CarPose,
        currentPose: CarPose,
        calibration: TrackCalibration
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
        calibration: TrackCalibration
    ) {
        val crossing = gateDetector.detectCrossing(
            previousPose,
            currentPose,
            calibration.startFinish,
        )

        if (crossing != null && crossing.isForwardDirection && state.canTriggerGate(timestampNs, GATE_START_FINISH)) {
            if (state.isLapRunning && state.isSyncedToStartFinish) {
                val expectedFinalSector = getSectorCount(calibration)
                val isValidSequence = state.currentSectorIndex == expectedFinalSector

                if (isValidSequence) {
                    state.completeLap(timestampNs, crossing.interpolationFactor)
                } else {
                    println("LAP: ⚠ Start/Finish crossed in sector ${state.currentSectorIndex}, expected $expectedFinalSector - re-syncing")
                    state.syncToStartFinish(timestampNs, crossing.interpolationFactor)
                }
            } else {
                state.syncToStartFinish(timestampNs, crossing.interpolationFactor)
            }
            state.markGateTriggered(timestampNs, GATE_START_FINISH)
        }
    }

    private fun findNextSectorFinishGate(calibration: TrackCalibration, sectorNumber: Int): Gate? {
        val totalSectors = getSectorCount(calibration)

        if (sectorNumber >= totalSectors) return null

        return calibration.sectors.firstOrNull { it.index == sectorNumber }?.finish
    }

    private fun getSectorCount(calibration: TrackCalibration): Int {
        return calibration.sectors.size.coerceAtLeast(1)
    }

    private companion object {

        const val GATE_START_FINISH = "SF"
    }
}
