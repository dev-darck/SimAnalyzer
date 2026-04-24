package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint

internal data class TrackCalibrationDraft(
    val trackId: String,
    val trackName: String,
    val referencePoint: ReferencePoint,
    val startFinish: Gate,
    val sectorStartMarks: List<Gate>,
)
