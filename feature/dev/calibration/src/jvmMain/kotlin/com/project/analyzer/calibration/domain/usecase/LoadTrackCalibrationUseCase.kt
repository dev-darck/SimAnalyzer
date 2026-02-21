package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject

@Inject
class LoadTrackCalibrationUseCase(private val repo: TrackCalibrationRepository) {

    suspend fun load(trackId: String): TrackCalibration? = repo.load(trackId)
}
