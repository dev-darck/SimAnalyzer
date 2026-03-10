package com.project.analyzer.calibration.domain

import com.project.analyzer.calibration.domain.model.CalibrationSample
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import kotlinx.coroutines.flow.StateFlow

interface TelemetrySampleProvider {

    val sample: StateFlow<CalibrationSample>
    fun setReferencePoint(point: ReferencePoint)
}
