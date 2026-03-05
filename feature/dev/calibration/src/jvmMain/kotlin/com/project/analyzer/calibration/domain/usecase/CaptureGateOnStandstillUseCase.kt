package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate

class GateCaptureException(message: String) : IllegalStateException(message)

data class GateCaptureResult(
    val gate: Gate,
    val capturedPosition: Vec2,
    val capturedForward: Vec2,
    val sampleCount: Int,
    val positionStdMeters: Float,
)

internal interface CaptureGateOnStandstillUseCase {
    suspend fun captureWithDetails(
        halfWidthMeters: Float = 10f,
        waitStableMs: Long = 600L,
        captureMs: Long = 1500L,
        speedThresholdKmh: Float = 2f,
        maxDriftMeters: Float = 0.5f,
        maxPosStdMeters: Float = 0.05f,
    ): GateCaptureResult
}
