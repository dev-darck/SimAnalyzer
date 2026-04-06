package com.analyzer.session.analysis.presentation.pipeline

/**
 * Interpolates comparison samples so charts and cursor queries can read smooth values between raw points.
 */
internal class SessionAnalysisInterpolator(private val points: List<SessionAnalysisComparisonSample>) {

    fun at(trackPosition: Float): SessionAnalysisComparisonSample {
        val clamped = trackPosition.coerceIn(0f, 1f)
        val exactIndex = points.indexOfFirst { point -> point.trackPosition >= clamped }
        if (exactIndex == -1) return points.last()
        if (exactIndex <= 0) return points.first()

        val previous = points[exactIndex - 1]
        val next = points[exactIndex]
        val distance = (next.trackPosition - previous.trackPosition).takeIf { it > 0.0001f } ?: return next
        val localFraction = ((clamped - previous.trackPosition) / distance).coerceIn(0f, 1f)

        return SessionAnalysisComparisonSample(
            trackPosition = clamped,
            frameId = if (localFraction < 0.5f) previous.frameId else next.frameId,
            elapsedMs = interpolateInt(previous.elapsedMs, next.elapsedMs, localFraction),
            speedKmh = interpolateFloat(previous.speedKmh, next.speedKmh, localFraction),
            throttle = interpolateFloat(previous.throttle, next.throttle, localFraction),
            brake = interpolateFloat(previous.brake, next.brake, localFraction),
            steeringAngleRad = interpolateFloat(previous.steeringAngleRad, next.steeringAngleRad, localFraction),
            lateralG = interpolateFloat(previous.lateralG, next.lateralG, localFraction),
            yawRateRad = interpolateFloat(previous.yawRateRad, next.yawRateRad, localFraction),
            gear = if (localFraction < 0.5f) previous.gear else next.gear,
            rpm = interpolateFloat(previous.rpm, next.rpm, localFraction),
            fuelLiters = interpolateFloat(previous.fuelLiters, next.fuelLiters, localFraction),
        )
    }
}
