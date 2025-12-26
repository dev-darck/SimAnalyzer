package com.project.analyzer.calibration.domain

import com.project.analyzer.calibration.data.model.CalibrationSample
import kotlinx.coroutines.flow.StateFlow

interface TelemetrySampleProvider {
    val sample: StateFlow<CalibrationSample>
}
