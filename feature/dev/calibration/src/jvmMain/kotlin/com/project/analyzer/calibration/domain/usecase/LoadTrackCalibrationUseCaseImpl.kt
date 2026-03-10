package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationWorkspace
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(ScreenScope::class)
@Inject
internal class LoadTrackCalibrationUseCaseImpl(private val calibrationWorkspace: TrackCalibrationWorkspace) :
    LoadTrackCalibrationUseCase {

    override suspend fun load(trackId: String, layoutId: String?): TrackCalibration? =
        calibrationWorkspace.loadEffective(
            trackId = trackId,
            layoutId = layoutId,
        )
}
