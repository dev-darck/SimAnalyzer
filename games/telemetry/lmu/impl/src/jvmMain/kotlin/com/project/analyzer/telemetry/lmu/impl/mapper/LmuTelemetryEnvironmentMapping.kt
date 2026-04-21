package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.api.model.environment.EnvironmentFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuScoringInfo
import kotlin.math.sqrt

internal fun mapEnvironment(scoringInfo: LmuScoringInfo?): EnvironmentFrame? {
    scoringInfo ?: return null
    return EnvironmentFrame(
        airTempC = scoringInfo.ambientTemp.toFloat().takeIf(Float::isFinite),
        roadTempC = scoringInfo.trackTemp.toFloat().takeIf(Float::isFinite),
        rainIntensity = scoringInfo.raining.toFloat().takeIf(Float::isFinite)?.coerceIn(0f, 1f),
        surfaceGrip = (1.0 - scoringInfo.avgPathWetness)
            .toFloat()
            .takeIf(Float::isFinite)
            ?.coerceIn(0f, 1f),
        windSpeedMps = scoringInfo.wind.speed(),
    )
}

private fun Vec3.speed(): Float = sqrt(x * x + y * y + z * z).takeIf(Float::isFinite) ?: 0f
