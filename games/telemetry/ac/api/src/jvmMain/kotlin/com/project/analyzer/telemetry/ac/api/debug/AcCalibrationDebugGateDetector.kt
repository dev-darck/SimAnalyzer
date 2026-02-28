package com.project.analyzer.telemetry.ac.api.debug

import com.project.analyzer.telemetry.ac.api.model.calibration.Gate

public interface AcCalibrationDebugGateDetector {

    public fun hasCrossing(
        previousPose: AcCalibrationCarPose,
        currentPose: AcCalibrationCarPose,
        gate: Gate,
    ): Boolean
}
