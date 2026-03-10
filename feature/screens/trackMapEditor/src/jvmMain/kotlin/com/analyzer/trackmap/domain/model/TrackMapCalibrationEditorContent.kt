package com.analyzer.trackmap.domain.model

data class TrackMapCalibrationEditorContent(
    val item: TrackMapLibraryItem,
    val snapshot: TrackMapCalibrationEditorSnapshot,
    val message: String,
)
