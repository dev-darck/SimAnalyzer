package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.handling.SessionAnalysisHandlingState

internal data class CornerSampleStatistics(
    val peakBrake: Float,
    val minSpeed: Float,
    val lateralG: Float,
    val trailBrakingIntensity: Float,
    val throttleSmoothness: Float,
    val wheelSpinDuration: Long,
    val handlingState: SessionAnalysisHandlingState,
    val slipAngle: Float,
)
