package com.analyzer.trackmap.domain.model

import com.project.analyzer.telemetry.ac.api.model.calibration.Gate

data class TrackMapCalibrationEditorGate(
    val id: String,
    val order: Int,
    val shortLabel: String,
    val title: String,
    val gate: Gate,
)
