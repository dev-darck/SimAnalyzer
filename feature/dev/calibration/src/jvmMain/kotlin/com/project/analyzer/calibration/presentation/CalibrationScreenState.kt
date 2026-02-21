package com.project.analyzer.calibration.presentation

sealed class CalibrationScreenState {
    object Calibration : CalibrationScreenState()
    data class Verify(val trackId: String) : CalibrationScreenState()
}
