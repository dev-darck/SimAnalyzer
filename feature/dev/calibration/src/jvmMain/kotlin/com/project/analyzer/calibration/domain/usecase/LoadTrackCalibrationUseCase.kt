package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

internal interface LoadTrackCalibrationUseCase {

    suspend fun load(trackId: String): TrackCalibration?
}
