package com.project.analyzer.telemetry.ac.api.calibration

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource

public interface TrackCalibrationRepository {

    public suspend fun save(
        calibration: TrackCalibration,
        source: TrackCalibrationSource = TrackCalibrationSource.USER,
    )

    public suspend fun load(trackId: String, layoutId: String? = null): TrackCalibration?

    public suspend fun loadBySource(
        trackId: String,
        source: TrackCalibrationSource,
        layoutId: String? = null,
    ): TrackCalibration?

    public suspend fun loadAll(source: TrackCalibrationSource? = null): List<TrackCalibration>
}
