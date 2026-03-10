package com.analyzer.trackmap.data.calibration

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

interface TrackMapCalibrationRepository {

    suspend fun load(trackId: String, layoutId: String? = null): TrackCalibration?
    suspend fun save(calibration: TrackCalibration)
}
