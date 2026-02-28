package com.project.analyzer.ac.telemetry.impl.fallback.debug

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.detector.GateCrossingDetector
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationCarPose
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationDebugGateDetector
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationDebugLapAnalyzer
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationLapTimingSnapshot
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot as ImplLapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.pose.model.CarPose as ImplCarPose

internal class AcCalibrationDebugLapAnalyzerAdapter(private val delegate: FallbackLapAnalyzer) :
    AcCalibrationDebugLapAnalyzer {

    override fun loadCalibration(trackId: String, calibration: TrackCalibration) {
        delegate.loadCalibration(trackId, calibration)
    }

    override fun processPose(timestampNs: Long, pose: AcCalibrationCarPose, calibration: TrackCalibration) {
        delegate.processPose(timestampNs, pose.toImpl(), calibration)
    }

    override fun getSnapshot(currentTimeNs: Long): AcCalibrationLapTimingSnapshot =
        delegate.getSnapshot(currentTimeNs).toApi()

    override fun resetSession() {
        delegate.resetSession()
    }
}

internal class AcCalibrationDebugGateDetectorAdapter(private val delegate: GateCrossingDetector) :
    AcCalibrationDebugGateDetector {

    override fun hasCrossing(
        previousPose: AcCalibrationCarPose,
        currentPose: AcCalibrationCarPose,
        gate: Gate,
    ): Boolean = delegate.detectCrossing(previousPose.toImpl(), currentPose.toImpl(), gate) != null
}

private fun AcCalibrationCarPose.toImpl(): ImplCarPose = ImplCarPose(
    position = position,
    velocityDir = velocityDir,
    headingDir = headingDir,
    speedKmh = speedKmh,
    isMovingForward = isMovingForward,
)

private fun ImplLapTimingSnapshot.toApi(): AcCalibrationLapTimingSnapshot = AcCalibrationLapTimingSnapshot(
    isActive = isActive,
    trackId = trackId,
    isLapRunning = isLapRunning,
    completedLapsCount = completedLapsCount,
    currentLapTimeMs = currentLapTimeMs,
    currentSectorTimeMs = currentSectorTimeMs,
    currentSectorIndex = currentSectorIndex,
    lastSectorTimeMs = lastSectorTimeMs,
    lastLapTimeMs = lastLapTimeMs,
    bestLapTimeMs = bestLapTimeMs,
    lastSectorsMs = lastSectorsMs,
    bestSectorsMs = bestSectorsMs,
    currentLapValid = currentLapValid,
    deltaLapTimeMs = deltaLapTimeMs,
    isDeltaPositive = isDeltaPositive,
    startFinishSyncId = startFinishSyncId,
)
