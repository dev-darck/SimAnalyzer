package com.project.analyzer.telemetry.analysis.impl.domain.resolver.handling

import com.project.analyzer.telemetry.analysis.api.model.handling.SessionAnalysisHandlingState
import dev.zacsweers.metro.Inject
import kotlin.math.abs

/**
 * Classifies the current balance state so understeer and oversteer feedback use one shared interpretation.
 */
@Inject
class SessionAnalysisHandlingStateResolver {

    fun resolve(
        speedKmh: Float?,
        steeringAngleRad: Float?,
        lateralG: Float?,
        yawRateRad: Float?,
        throttle: Float?,
        brake: Float?,
    ): SessionAnalysisHandlingState {
        val speed = speedKmh ?: return SessionAnalysisHandlingState.Neutral
        val steering = abs(steeringAngleRad ?: return SessionAnalysisHandlingState.Neutral)
        val lateral = abs(lateralG ?: return SessionAnalysisHandlingState.Neutral)
        val yaw = abs(yawRateRad ?: return SessionAnalysisHandlingState.Neutral)

        if (speed < 60f || steering < 0.06f) return SessionAnalysisHandlingState.Neutral

        val demand = steering * (speed / 110f).coerceAtLeast(0.6f)
        val response = lateral * 0.85f + yaw * 0.65f
        val brakeValue = brake ?: 0f
        val throttleValue = throttle ?: 0f

        return when {
            brakeValue < 0.45f && response < demand * 0.82f -> SessionAnalysisHandlingState.Understeer
            throttleValue < 0.95f && response > demand * 1.55f -> SessionAnalysisHandlingState.Oversteer
            else -> SessionAnalysisHandlingState.Neutral
        }
    }
}
