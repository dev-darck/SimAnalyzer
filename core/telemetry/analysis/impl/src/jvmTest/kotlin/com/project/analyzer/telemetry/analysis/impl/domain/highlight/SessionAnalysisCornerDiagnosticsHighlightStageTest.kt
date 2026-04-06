package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreCompoundFamily
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisConsistencyResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.AeroBalanceDiagnosticResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.BalanceDiagnosticResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.BrakeBiasDiagnosticResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.DamperDiagnosticResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnosticResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.TyrePressureDiagnosticResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.TyreTemperatureDiagnosticResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone
import kotlinx.coroutines.runBlocking
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionAnalysisCornerDiagnosticsHighlightStageTest {

    private val stage = SessionAnalysisCornerDiagnosticsHighlightStage(
        cornerAnalysisResolver = SessionAnalysisCornerAnalysisResolver(),
        setupDiagnosticResolver = SessionAnalysisSetupDiagnosticResolver(
            balanceDiagnosticResolver = BalanceDiagnosticResolver(),
            tyreTemperatureDiagnosticResolver = TyreTemperatureDiagnosticResolver(),
            tyrePressureDiagnosticResolver = TyrePressureDiagnosticResolver(),
            brakeBiasDiagnosticResolver = BrakeBiasDiagnosticResolver(),
            aeroBalanceDiagnosticResolver = AeroBalanceDiagnosticResolver(),
            damperDiagnosticResolver = DamperDiagnosticResolver(),
        ),
        consistencyResolver = SessionAnalysisConsistencyResolver(),
    )

    @Test
    fun `execute populates corner consistency and setup reports from telemetry input`() = runBlocking {
        val result = stage.execute(
            SessionAnalysisHighlightContext(
                samples = buildReferenceLap() + buildLateApexLap(),
                bestLapBySegmentId = mapOf(1L to 1),
                tyreProfile = gt3TyreProfile(),
                cornerZonesBySegmentId = mapOf(
                    1L to listOf(
                        TrackMapCornerZone(
                            cornerNumber = 4,
                            startTrackPosition = 0.08f,
                            endTrackPosition = 0.34f,
                            apexTrackPosition = 0.24f,
                            peakCurvature = 0.028f,
                        ),
                    ),
                ),
            ),
        )

        val analyzedCorner = result.cornerReport?.corners?.singleOrNull { corner -> corner.lapNumber == 2 }

        assertNotNull(result.cornerReport)
        assertNotNull(result.setupReport)
        assertNotNull(result.consistencyReport)
        assertNotNull(analyzedCorner)
        assertEquals(4, analyzedCorner.cornerNumber)
        assertTrue(result.consistencyReport.overallScore < 100)
    }
}

private fun buildReferenceLap() = buildLapSamples(
    lapNumber = 1,
    fractions = listOf(0.05f, 0.08f, 0.12f, 0.16f, 0.20f, 0.24f, 0.28f, 0.32f, 0.36f, 0.40f),
    speeds = listOf(170f, 166f, 150f, 132f, 112f, 95f, 106f, 124f, 146f, 160f),
    steering = listOf(0f, 0.07f, 0.10f, 0.12f, 0.12f, 0.11f, 0.09f, 0.06f, 0.02f, 0f),
    lateral = listOf(0.1f, 0.82f, 0.98f, 1.08f, 1.18f, 1.15f, 0.96f, 0.74f, 0.32f, 0.12f),
    yaw = listOf(0f, 0.22f, 0.26f, 0.30f, 0.32f, 0.29f, 0.25f, 0.20f, 0.08f, 0f),
    brake = listOf(0f, 0.32f, 0.30f, 0.24f, 0.18f, 0.10f, 0.06f, 0f, 0f, 0f),
    throttle = listOf(1f, 0.22f, 0f, 0f, 0f, 0.08f, 0.22f, 0.46f, 0.84f, 1f),
    delta = listOf(0, 0, 2, 4, 6, 8, 8, 8, 8, 8),
)

private fun buildLateApexLap() = buildLapSamples(
    lapNumber = 2,
    fractions = listOf(0.05f, 0.08f, 0.12f, 0.16f, 0.20f, 0.24f, 0.28f, 0.32f, 0.36f, 0.40f),
    speeds = listOf(170f, 168f, 160f, 148f, 136f, 120f, 90f, 102f, 126f, 148f),
    steering = listOf(0f, 0.07f, 0.10f, 0.12f, 0.13f, 0.13f, 0.11f, 0.09f, 0.03f, 0f),
    lateral = listOf(0.1f, 0.86f, 1.00f, 1.08f, 1.12f, 1.18f, 1.22f, 0.92f, 0.38f, 0.12f),
    yaw = listOf(0f, 0.22f, 0.26f, 0.29f, 0.31f, 0.33f, 0.34f, 0.24f, 0.10f, 0f),
    brake = listOf(0f, 0.92f, 0.82f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    throttle = listOf(1f, 0.12f, 0f, 0f, 0f, 0.04f, 0.08f, 0.30f, 0.92f, 1f),
    delta = listOf(0, 12, 28, 46, 74, 104, 138, 156, 164, 170),
    frontSlip = listOf(0.04f, 0.24f, 0.22f, 0.05f, 0.04f, 0.04f, 0.04f, 0.05f, 0.05f, 0.04f),
)

private fun buildLapSamples(
    lapNumber: Int,
    fractions: List<Float>,
    speeds: List<Float>,
    steering: List<Float>,
    lateral: List<Float>,
    yaw: List<Float>,
    brake: List<Float>,
    throttle: List<Float>,
    delta: List<Int>,
    frontSlip: List<Float> = List(fractions.size) { 0.04f },
    rearSlip: List<Float> = List(fractions.size) { 0.03f },
) = fractions.indices.map { index ->
    highlightSample(
        lapNumber = lapNumber,
        sampleIndexInLap = index,
        trackPosition = fractions[index],
        speedKmh = speeds[index],
        throttle = throttle[index],
        brake = brake[index],
        deltaToBestMs = delta[index],
    ).copy(
        segmentId = 1L,
        frameId = lapNumber * 1_000L + index.toLong(),
        timestampNs = lapNumber * 1_000_000_000L + index * 16_000_000L,
        trackX = fractions[index] * 1_000f,
        trackY = sin(fractions[index] * PI.toFloat() * 2f) * 10f,
        steeringAngleRad = steering[index],
        lateralG = lateral[index],
        yawRateRad = yaw[index],
        tyreFl = SessionAnalysisTyreState(slip = frontSlip[index], pressurePsi = 28.2f, coreTempC = 102f),
        tyreFr = SessionAnalysisTyreState(slip = frontSlip[index], pressurePsi = 28.0f, coreTempC = 101f),
        tyreRl = SessionAnalysisTyreState(slip = rearSlip[index], pressurePsi = 26.5f, coreTempC = 94f),
        tyreRr = SessionAnalysisTyreState(slip = rearSlip[index], pressurePsi = 26.3f, coreTempC = 94f),
    )
}

private fun gt3TyreProfile() = SessionAnalysisTyreProfile(
    vehicleClass = SessionAnalysisVehicleClass.GT3,
    compoundFamily = SessionAnalysisTyreCompoundFamily.Medium,
    surfaceOptimalMinC = 75f,
    surfaceOptimalMaxC = 95f,
    coreOptimalMinC = 80f,
    coreOptimalMaxC = 100f,
    brakeOptimalMinC = 350f,
    brakeOptimalMaxC = 520f,
    pressureOptimalMinPsi = 26f,
    pressureOptimalMaxPsi = 27f,
    innerOuterSpreadWarnC = 6f,
    innerOuterSpreadCriticalC = 10f,
)
