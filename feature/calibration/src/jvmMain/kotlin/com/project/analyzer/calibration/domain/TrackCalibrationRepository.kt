package com.project.analyzer.calibration.domain

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

interface TrackCalibrationRepository {

    fun setPath(path: String)
    suspend fun save(calibration: TrackCalibration)
    suspend fun load(trackId: String): TrackCalibration?
    suspend fun loadAll(): List<TrackCalibration>?
}
