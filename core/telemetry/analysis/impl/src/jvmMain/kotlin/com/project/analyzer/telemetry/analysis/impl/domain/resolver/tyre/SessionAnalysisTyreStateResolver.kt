package com.project.analyzer.telemetry.analysis.impl.domain.resolver.tyre

import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreTemperatureBand
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryTyreSnapshot
import dev.zacsweers.metro.Inject

/**
 * Resolves live tyre telemetry into window state and degradation signals that other diagnostics can reuse.
 */
@Inject
class SessionAnalysisTyreStateResolver {

    fun resolve(
        snapshot: RecordedTelemetryTyreSnapshot?,
        profile: SessionAnalysisTyreProfile?,
    ): SessionAnalysisTyreState? {
        if (snapshot == null) return null
        return SessionAnalysisTyreState(
            pressurePsi = snapshot.pressurePsi,
            coreTempC = snapshot.coreTempC,
            innerTempC = snapshot.innerTempC,
            middleTempC = snapshot.middleTempC,
            outerTempC = snapshot.outerTempC,
            avgTempC = snapshot.avgTempC,
            brakeTempC = snapshot.brakeTempC,
            slip = snapshot.slip,
            load = snapshot.load,
            tempBand = resolveBand(
                avgTempC = snapshot.avgTempC ?: snapshot.middleTempC ?: snapshot.coreTempC,
                profile = profile,
            ),
        )
    }

    fun resolveBand(avgTempC: Float?, profile: SessionAnalysisTyreProfile?): SessionAnalysisTyreTemperatureBand? {
        if (avgTempC == null || profile == null) return null

        val optimalMin = profile.surfaceOptimalMinC
        val optimalMax = profile.surfaceOptimalMaxC
        return when {
            avgTempC < optimalMin - 18f -> SessionAnalysisTyreTemperatureBand.Cold
            avgTempC < optimalMin - 8f -> SessionAnalysisTyreTemperatureBand.Warming
            avgTempC <= optimalMax -> SessionAnalysisTyreTemperatureBand.Optimal
            avgTempC <= optimalMax + 6f -> SessionAnalysisTyreTemperatureBand.Warm
            avgTempC <= optimalMax + 12f -> SessionAnalysisTyreTemperatureBand.Hot
            avgTempC <= optimalMax + 20f -> SessionAnalysisTyreTemperatureBand.Overheated
            else -> SessionAnalysisTyreTemperatureBand.Critical
        }
    }
}
