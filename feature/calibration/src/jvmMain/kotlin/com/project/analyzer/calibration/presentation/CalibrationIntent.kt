package com.project.analyzer.calibration.presentation

import com.project.analyzer.calibration.data.model.ReferencePoint

sealed interface CalibrationIntent {
    data class TrackNameChanged(val value: String) : CalibrationIntent
    data class ReferencePointChanged(val value: ReferencePoint) : CalibrationIntent
    data class TriggerRadiusChanged(val meters: Float) : CalibrationIntent
    data class DebugWidthChanged(val halfWidthMeters: Float) : CalibrationIntent
    data class SetFileToSave(val path: String) : CalibrationIntent
    data object CaptureStartFinish : CalibrationIntent
    data object CaptureSplit1 : CalibrationIntent
    data object CaptureSplit2 : CalibrationIntent
    data class CaptureSectorStart(val index: Int) : CalibrationIntent
    data class CaptureSectorFinish(val index: Int) : CalibrationIntent

    data object Save : CalibrationIntent
    data object Reset : CalibrationIntent
}
