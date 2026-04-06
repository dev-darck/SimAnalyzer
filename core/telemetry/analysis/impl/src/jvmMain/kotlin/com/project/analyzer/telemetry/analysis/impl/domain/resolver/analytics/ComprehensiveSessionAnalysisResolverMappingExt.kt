package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.report.analysis.AccelerationAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.BrakingAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.SteeringAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.common.WheelPosition
import com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerIssue
import com.project.analyzer.telemetry.analysis.api.model.report.corner.EnhancedCornerAnalysis
import kotlin.math.roundToLong

// Heuristics below translate normalized corner metrics into public report fields on a readable scale.
private const val brakingAveragePressureRatio: Float = 0.65f
private const val maxTimeToFullThrottleMs: Float = 850f
private const val slipAngleInflationFactor: Float = 1.2f
private const val consistencyScoreBins: Int = 20

/**
 * Converts existing analysis primitives into the richer public report models without duplicating detector logic.
 */
internal fun cornerToBraking(corner: EnhancedCornerAnalysis): BrakingAnalysis = BrakingAnalysis(
    zonePosition = corner.brakePointPosition,
    entrySpeed = corner.entrySpeed,
    cornerSpeed = corner.apexSpeed,
    speedReduction = (corner.entrySpeed - corner.apexSpeed).coerceAtLeast(0f),
    brakePoint = corner.brakePointPosition,
    referenceBrakePoint = corner.referenceBrakePoint,
    brakePointDelta = corner.brakePointDelta,
    peakBrakePressure = corner.peakBrake,
    avgBrakePressure = corner.peakBrake * brakingAveragePressureRatio,
    brakeSmoothness = corner.throttleSmoothness,
    brakeTrailRatio = corner.trailBrakingIntensity,
    lockupDetected = corner.primaryIssue == CornerIssue.LOCKUP_BRAKING,
    lockupWheel = if (corner.primaryIssue == CornerIssue.LOCKUP_BRAKING) WheelPosition.FRONT_LEFT else null,
    lockupDuration = corner.wheelSpinDuration,
    absActivation = corner.primaryIssue == CornerIssue.LOCKUP_BRAKING,
    absActivationDuration = corner.wheelSpinDuration,
    timeDelta = corner.timeDelta,
    distanceDelta = corner.brakePointDelta * ComprehensiveSessionAnalysisResolverConstants.DefaultTrackLengthMeters,
    recommendation = corner.recommendation,
    severity = corner.timeDelta.toIssueSeverity(),
)

internal fun cornerToAcceleration(corner: EnhancedCornerAnalysis): AccelerationAnalysis = AccelerationAnalysis(
    zonePosition = corner.throttlePickupPoint,
    exitSpeed = corner.exitSpeed,
    referenceExitSpeed = corner.referenceExitSpeed,
    throttlePickupPoint = corner.throttlePickupPoint,
    throttlePickupSmoothness = corner.throttleSmoothness,
    avgThrottle = 0.7f,
    timeToFullThrottle = ((1f - corner.throttleSmoothness) * maxTimeToFullThrottleMs).roundToLong(),
    wheelSpinDetected = corner.wheelSpinDetected,
    wheelSpinWheels = if (corner.wheelSpinDetected) {
        listOf(
            WheelPosition.REAR_LEFT,
            WheelPosition.REAR_RIGHT,
        )
    } else {
        emptyList()
    },
    wheelSpinSeverity = if (corner.wheelSpinDetected) (1f - corner.throttleSmoothness).coerceAtLeast(0f) else 0f,
    wheelSpinDuration = corner.wheelSpinDuration,
    tractionControlActive = corner.wheelSpinDetected,
    tractionControlInterventions = if (corner.wheelSpinDetected) 1 else 0,
    understeerOnExit = corner.primaryIssue == CornerIssue.EARLY_THROTTLE,
    oversteerOnExit = corner.primaryIssue == CornerIssue.OVERSTEER_EXIT,
    timeDelta = corner.timeDelta,
    speedDelta = corner.exitSpeed - corner.referenceExitSpeed,
    recommendation = corner.recommendation,
    severity = corner.timeDelta.toIssueSeverity(),
)

internal fun cornerToSteering(corner: EnhancedCornerAnalysis): SteeringAnalysis = SteeringAnalysis(
    segmentPosition = corner.apexPosition,
    avgSteeringAngle = corner.slipAngle,
    maxSteeringAngle = corner.slipAngle * slipAngleInflationFactor,
    steeringSmoothness = corner.throttleSmoothness,
    lineDeviation = corner.speedStdDev,
    referenceLineDeviation = 0f,
    inputCount = corner.consistencyScore / consistencyScoreBins,
    sawtoothDetected = corner.consistencyScore < 70,
    understeerDetected = corner.understeerRatio >= 0.25f,
    understeerSeverity = corner.understeerRatio,
    oversteerDetected = corner.oversteerRatio >= 0.25f,
    oversteerSeverity = corner.oversteerRatio,
    recommendation = corner.recommendation,
    severity = corner.timeDelta.toIssueSeverity(),
)
