package com.project.analyzer.calibration.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.project.analyzer.calibration.presentation.setup.CalibrationScreen
import com.project.analyzer.calibration.presentation.verify.CalibrationVerifyScreen

sealed class Screen {
    object Calibration : Screen()
    data class Verify(val trackId: String) : Screen()
}

@Composable
fun CalibrationHost() {
    var screen by remember { mutableStateOf<Screen>(Screen.Calibration) }

    when (val s = screen) {
        is Screen.Calibration -> CalibrationScreen(
            onVerify = { trackId -> screen = Screen.Verify(trackId) }
        )

        is Screen.Verify -> CalibrationVerifyScreen(
            trackId = s.trackId,
            onBack = { screen = Screen.Calibration }
        )
    }
}
