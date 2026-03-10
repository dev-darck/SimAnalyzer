package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.ac.telemetry.impl.fallback.TrackCalibrationLoader
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationWorkspace
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TrackCalibrationWorkspace>())
@Inject
@Suppress("unused")
class TrackCalibrationWorkspaceImpl internal constructor(
    private val repository: TrackCalibrationRepository,
    private val loader: TrackCalibrationLoader,
    private val bundledExporter: TrackCalibrationBundledExporter,
) : TrackCalibrationWorkspace {

    override suspend fun loadEffective(trackId: String, layoutId: String?): TrackCalibration? = loader.load(
        trackId = trackId,
        layoutId = layoutId,
    )

    override suspend fun save(calibration: TrackCalibration) {
        val userCalibration = calibration.copy(source = TrackCalibrationSource.USER)
        repository.save(
            calibration = userCalibration,
            source = TrackCalibrationSource.USER,
        )
        loader.cache(userCalibration)
        bundledExporter.export(userCalibration)
    }
}
