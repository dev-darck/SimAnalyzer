package com.project.analyzer.calibration.domain

import com.project.analyzer.calibration.data.model.TrackCalibration

interface TrackCalibrationRepository {

    fun setPath(path: String)
    suspend fun save(calibration: TrackCalibration)
    suspend fun load(trackId: String): TrackCalibration?
    suspend fun loadAll(): List<TrackCalibration>
}
