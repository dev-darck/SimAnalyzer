package com.analyzer.trackmap.domain.model

import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource

data class TrackMapCalibrationEditorSnapshot(
    val referencePoint: ReferencePoint,
    val source: TrackCalibrationSource?,
    val gates: List<TrackMapCalibrationEditorGate>,
    val markers: List<TrackMapCalibrationEditorMarker>,
    val sectors: List<TrackMapCalibrationEditorSector>,
    val selectedMarkerId: String?,
)
