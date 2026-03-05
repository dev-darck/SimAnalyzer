package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject

@Inject
internal class LoadTrackCalibrationUseCaseImpl(
    private val repo: TrackCalibrationRepository,
) : LoadTrackCalibrationUseCase {

    override suspend fun load(trackId: String): TrackCalibration? = repo.load(trackId)
}
