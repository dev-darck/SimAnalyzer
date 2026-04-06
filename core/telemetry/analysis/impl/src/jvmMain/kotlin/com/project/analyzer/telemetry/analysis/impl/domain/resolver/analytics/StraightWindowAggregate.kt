package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

internal data class StraightWindowAggregate(
    val windowCount: Int,
    val speedAverageSum: Float,
    val minSpeed: Float,
    val maxSpeed: Float,
    val throttleSum: Float,
    val throttleCount: Int,
    val peakThrottle: Float,
    val brakeSum: Float,
    val brakeCount: Int,
    val peakBrake: Float,
    val steeringAbsSum: Float,
    val steeringCount: Int,
    val steeringValues: List<Float>,
    val durationSumMs: Float,
) {

    val averageSpeed: Float
        get() = if (windowCount == 0) 0f else speedAverageSum / windowCount.toFloat()

    val averageDurationMs: Float
        get() = if (windowCount == 0) 0f else durationSumMs / windowCount.toFloat()

    val averageThrottle: Float
        get() = if (throttleCount == 0) 0f else throttleSum / throttleCount.toFloat()

    val averageBrake: Float
        get() = if (brakeCount == 0) 0f else brakeSum / brakeCount.toFloat()

    val averageSteeringAngle: Float
        get() = if (steeringCount == 0) 0f else steeringAbsSum / steeringCount.toFloat()

    val resolvedMinSpeed: Float
        get() = minSpeed.takeUnless(Float::isInfinite) ?: 0f

    val resolvedMaxSpeed: Float
        get() = maxSpeed.takeUnless(Float::isInfinite) ?: 0f
}
