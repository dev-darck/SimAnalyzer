package com.analyzer.session.analysis.presentation.pipeline

internal data class SessionAnalysisComparisonSample(
    val trackPosition: Float,
    val frameId: Long,
    val elapsedMs: Int?,
    val speedKmh: Float?,
    val throttle: Float?,
    val brake: Float?,
    val steeringAngleRad: Float?,
    val lateralG: Float?,
    val yawRateRad: Float?,
    val gear: Int?,
    val rpm: Float?,
    val fuelLiters: Float?,
)
