package com.project.analyzer.telemetry.analysis.api.model.report.analysis

import com.project.analyzer.telemetry.analysis.api.model.report.common.IssueSeverity
import com.project.analyzer.telemetry.analysis.api.model.report.common.WheelPosition

public data class TyreAnalysis(
    val lapNumber: Int = 0,
    val avgCoreTemp: Float = 0f,
    val peakCoreTemp: Float = 0f,
    val tempSpread: Float = 0f,
    val innerOuterSpread: Float = 0f,
    val tyreState: TyreConditionState = TyreConditionState.COLD,
    val pressures: Map<WheelPosition, Float> = emptyMap(),
    val pressureSpread: Float = 0f,
    val optimalPressureDeviation: Float = 0f,
    val pressureBalance: PressureBalance = PressureBalance.OPTIMAL,
    val wearRate: Float = 0f,
    val estimatedLifespan: Int = 0,
    val wearBalance: WearBalance = WearBalance.EVEN,
    val avgSlipRatio: Float = 0f,
    val peakSlipRatio: Float = 0f,
    val slipConsistency: Float = 1f,
    val degradationLevel: Float = 0f,
    val performanceLoss: Float = 0f,
    val recommendations: List<String> = emptyList(),
    val severity: IssueSeverity = IssueSeverity.NEUTRAL,
)
