package com.project.analyzer.telemetry.ac.api.debug

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

public interface AcCalibrationDebugLapAnalyzer {

    public fun loadCalibration(trackId: String, calibration: TrackCalibration)

    public fun processPose(timestampNs: Long, pose: AcCalibrationCarPose, calibration: TrackCalibration)

    public fun getSnapshot(currentTimeNs: Long): AcCalibrationLapTimingSnapshot

    public fun resetSession()
}
