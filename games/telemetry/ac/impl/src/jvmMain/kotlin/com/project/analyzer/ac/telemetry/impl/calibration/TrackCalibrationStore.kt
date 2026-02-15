package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.annotations.DataStoreSerializer
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import kotlinx.serialization.Serializable

@Serializable
@DataStoreSerializer
internal data class TrackCalibrationStore(
    val calibrations: List<TrackCalibration> = emptyList()
)
