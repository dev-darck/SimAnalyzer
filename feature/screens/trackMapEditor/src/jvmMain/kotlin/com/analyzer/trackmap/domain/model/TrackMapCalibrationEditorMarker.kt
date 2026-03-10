package com.analyzer.trackmap.domain.model

data class TrackMapCalibrationEditorMarker(
    val gateId: String,
    val order: Int,
    val shortLabel: String,
    val title: String,
    val colorHex: Long,
    val meters: Float,
    val pointIndex: Int,
)
