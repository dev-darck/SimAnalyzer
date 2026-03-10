package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration

interface LoadTrackCalibrationUseCase {

    suspend fun load(trackId: String, layoutId: String? = null): TrackCalibration?
}
