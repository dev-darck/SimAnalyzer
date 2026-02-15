package com.project.analyzer.calibration.presentation.setup

import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint

sealed interface CalibrationIntent {
    data class TrackNameChanged(val value: String) : CalibrationIntent
    data class ReferencePointChanged(val value: ReferencePoint) : CalibrationIntent
    data class TriggerRadiusChanged(val meters: Float) : CalibrationIntent
    data object CaptureStartFinish : CalibrationIntent
    data object AddSector : CalibrationIntent
    data class CaptureSectorStart(val index: Int) : CalibrationIntent
    data class RemoveSector(val index: Int) : CalibrationIntent
    data object FlipStartFinishDirection : CalibrationIntent
    data class FlipSectorStartDirection(val index: Int) : CalibrationIntent
    data object Save : CalibrationIntent
    data object Reset : CalibrationIntent
    data object LoadAllCalibrations : CalibrationIntent
}
