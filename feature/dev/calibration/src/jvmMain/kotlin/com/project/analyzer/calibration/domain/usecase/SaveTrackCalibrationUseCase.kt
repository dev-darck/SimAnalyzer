package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

internal interface SaveTrackCalibrationUseCase {

    suspend fun save(calibration: TrackCalibration)
    suspend fun save(draft: TrackCalibrationDraft)
    suspend fun loadAll(): List<String>
}
