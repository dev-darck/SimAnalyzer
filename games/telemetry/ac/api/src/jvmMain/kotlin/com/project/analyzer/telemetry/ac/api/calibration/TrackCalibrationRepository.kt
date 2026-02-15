package com.project.analyzer.telemetry.ac.api.calibration

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

public interface TrackCalibrationRepository {

    public suspend fun save(calibration: TrackCalibration)
    public suspend fun load(trackId: String): TrackCalibration?
    public suspend fun loadAll(): List<TrackCalibration>
}
