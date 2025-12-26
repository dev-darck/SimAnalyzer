package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.calibration.data.model.TrackCalibration
import com.project.analyzer.calibration.domain.TrackCalibrationRepository
import dev.zacsweers.metro.Inject

@Inject
class LoadTrackCalibrationUseCase(
    private val repo: TrackCalibrationRepository
) {
    suspend fun load(trackId: String): TrackCalibration? = repo.load(trackId)
}
