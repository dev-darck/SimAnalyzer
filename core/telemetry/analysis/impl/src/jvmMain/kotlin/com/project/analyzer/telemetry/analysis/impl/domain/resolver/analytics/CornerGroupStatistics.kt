package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerApexClassification

internal data class CornerGroupStatistics(
    val averageBrakePoint: Float,
    val averageThrottlePickup: Float,
    val averageApexPosition: Float,
    val entrySpeed: Float,
    val apexSpeed: Float,
    val exitSpeed: Float,
    val averageScore: Int,
    val averageUndersteer: Float,
    val averageOversteer: Float,
    val speedStdDev: Float,
    val dominantApexClassification: SessionAnalysisCornerApexClassification,
    val hasWheelSpin: Boolean,
    val hasWheelLockup: Boolean,
    val wheelSpinShare: Float,
    val wheelLockupShare: Float,
    val referenceTime: Long,
    val timeDelta: Long,
    val cornerTime: Long,
)
