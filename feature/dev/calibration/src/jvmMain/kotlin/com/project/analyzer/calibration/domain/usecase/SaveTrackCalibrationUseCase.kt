package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(ScreenScope::class)
class SaveTrackCalibrationUseCase(private val repo: TrackCalibrationRepository) {

    suspend fun save(calibration: TrackCalibration) = repo.save(calibration)

    suspend fun loadAll(): List<String> = repo.loadAll().map { it.trackId }
}
