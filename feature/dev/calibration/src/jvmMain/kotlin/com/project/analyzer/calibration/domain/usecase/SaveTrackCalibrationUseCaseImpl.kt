package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationWorkspace
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(ScreenScope::class)
@Inject
internal class SaveTrackCalibrationUseCaseImpl(
    private val repo: TrackCalibrationRepository,
    private val calibrationWorkspace: TrackCalibrationWorkspace,
) : SaveTrackCalibrationUseCase {

    override suspend fun save(calibration: TrackCalibration) {
        calibrationWorkspace.save(calibration)
    }

    override suspend fun loadAll(): List<String> = repo.loadAll()
        .map { calibration ->
            buildString {
                append(calibration.trackId)
                calibration.layoutId?.takeIf { it.isNotBlank() }?.let { layoutId ->
                    append(" [")
                    append(layoutId)
                    append(']')
                }
            }
        }
        .distinct()
}
