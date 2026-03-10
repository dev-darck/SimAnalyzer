package com.analyzer.trackmap.domain.model

data class TrackMapCalibrationEditorSector(
    val index: Int,
    val name: String,
    val colorHex: Long,
    val startGateId: String,
    val endGateId: String,
    val startMeters: Float,
    val endMeters: Float,
    val startPointIndex: Int,
    val endPointIndex: Int,
)
