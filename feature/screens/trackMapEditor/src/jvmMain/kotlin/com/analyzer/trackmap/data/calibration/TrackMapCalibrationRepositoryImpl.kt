package com.analyzer.trackmap.data.calibration

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationWorkspace
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@SingleIn(ScreenScope::class)
@ContributesBinding(ScreenScope::class, binding = binding<TrackMapCalibrationRepository>())
@Inject
class TrackMapCalibrationRepositoryImpl(private val calibrationWorkspace: TrackCalibrationWorkspace) :
    TrackMapCalibrationRepository {

    override suspend fun load(trackId: String, layoutId: String?): TrackCalibration? =
        calibrationWorkspace.loadEffective(
            trackId = trackId,
            layoutId = layoutId,
        )

    override suspend fun save(calibration: TrackCalibration) {
        calibrationWorkspace.save(calibration)
    }
}
