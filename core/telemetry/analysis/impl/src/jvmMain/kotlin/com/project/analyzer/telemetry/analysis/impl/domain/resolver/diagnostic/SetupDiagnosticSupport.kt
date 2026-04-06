package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState
import kotlin.math.abs

internal const val setupBalanceRatioWarn: Float = 0.32f
internal const val setupRepeatRatioThreshold: Float = 0.6f
internal const val setupPressureAxisWarnPsi: Float = 0.45f
internal const val setupPressureSideWarnPsi: Float = 0.35f
internal const val setupPressureWindowWarnPsi: Float = 0.4f
internal const val setupAeroSpeedThresholdKmh: Float = 150f
internal const val setupDamperLatDeltaWarn: Float = 0.55f
internal const val setupDamperYawDeltaWarn: Float = 0.22f
internal const val setupDamperRateWarn: Float = 0.12f

internal fun SessionAnalysisSample.hasFrontLockupSignal(): Boolean {
    if ((brake ?: 0f) < 0.8f) return false
    val frontSlip = listOfNotNull(tyreFl?.slip, tyreFr?.slip).maxOrNull() ?: return false
    val rearSlip = listOfNotNull(tyreRl?.slip, tyreRr?.slip).maxOrNull() ?: 0f
    return frontSlip >= 0.16f && frontSlip - rearSlip >= 0.06f
}

internal data class ResolvedTyreSample(
    val label: String,
    val sample: SessionAnalysisSample,
    val tyre: SessionAnalysisTyreState,
) {

    fun spreadMagnitude(): Float {
        val inner = tyre.innerTempC ?: tyre.middleTempC ?: return 0f
        val outer = tyre.outerTempC ?: tyre.middleTempC ?: return 0f
        return abs(inner - outer)
    }
}

internal fun List<Int>.averageIntOrNull(): Float? = if (isEmpty()) null else average().toFloat()
