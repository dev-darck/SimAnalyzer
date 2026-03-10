package com.project.analyzer.telemetry.ac.api.calibration

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

public interface TrackCalibrationWorkspace {

    public suspend fun loadEffective(trackId: String, layoutId: String? = null): TrackCalibration?

    public suspend fun save(calibration: TrackCalibration)
}
