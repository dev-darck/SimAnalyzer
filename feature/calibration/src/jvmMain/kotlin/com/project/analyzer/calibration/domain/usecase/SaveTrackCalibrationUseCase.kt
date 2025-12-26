package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.calibration.data.model.TrackCalibration
import com.project.analyzer.calibration.domain.TrackCalibrationRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class SaveTrackCalibrationUseCase(
    private val repo: TrackCalibrationRepository
) {
    fun setPath(path: String) = repo.setPath(path)
    suspend fun save(calibration: TrackCalibration) = repo.save(calibration)
}
