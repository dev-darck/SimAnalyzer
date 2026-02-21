package com.project.analyzer.calibration.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.project.analyzer.calibration.presentation.setup.CalibrationScreen
import com.project.analyzer.calibration.presentation.verify.CalibrationVerifyScreen

@Composable
fun CalibrationHost() {
    var screen by remember { mutableStateOf<CalibrationScreenState>(CalibrationScreenState.Calibration) }

    when (val s = screen) {
        is CalibrationScreenState.Calibration ->
            CalibrationScreen(
                onVerify = { trackId -> screen = CalibrationScreenState.Verify(trackId) },
            )

        is CalibrationScreenState.Verify ->
            CalibrationVerifyScreen(
                trackId = s.trackId,
                onBack = { screen = CalibrationScreenState.Calibration },
            )
    }
}
