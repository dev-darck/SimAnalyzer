package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreCompoundFamily
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertContains

class SessionAnalysisSetupDiagnosticResolverTest {

    private val resolver = SessionAnalysisSetupDiagnosticResolver(
        balanceDiagnosticResolver = BalanceDiagnosticResolver(),
        tyreTemperatureDiagnosticResolver = TyreTemperatureDiagnosticResolver(),
        tyrePressureDiagnosticResolver = TyrePressureDiagnosticResolver(),
        brakeBiasDiagnosticResolver = BrakeBiasDiagnosticResolver(),
        aeroBalanceDiagnosticResolver = AeroBalanceDiagnosticResolver(),
        damperDiagnosticResolver = DamperDiagnosticResolver(),
    )

    @Test
    fun `resolve emits chronic balance and tyre setup diagnostics`() = runBlocking {
        val report = resolver.resolve(
            corners = listOf(
                buildSetupCorner(lapNumber = 1, apexTrackPosition = 0.22f, timeLossMs = 88),
                buildSetupCorner(lapNumber = 2, apexTrackPosition = 0.23f, timeLossMs = 96),
                buildSetupCorner(lapNumber = 3, apexTrackPosition = 0.24f, timeLossMs = 104),
            ),
            samples = listOf(
                buildSetupSample(lapNumber = 1, trackPosition = 0.22f),
                buildSetupSample(lapNumber = 2, trackPosition = 0.23f),
                buildSetupSample(lapNumber = 3, trackPosition = 0.24f),
            ),
            tyreProfile = gt3TyreProfile(),
        )

        val categories = report.diagnostics.map { diagnostic -> diagnostic.category }
        val setupUndersteer = report.diagnostics.first { diagnostic ->
            diagnostic.category == SessionAnalysisHighlightCategory.SetupUndersteer
        }
        val pressure = report.diagnostics.first { diagnostic ->
            diagnostic.category == SessionAnalysisHighlightCategory.TyrePressureImbalance
        }

        assertTrue(SessionAnalysisHighlightCategory.SetupUndersteer in categories)
        assertTrue(SessionAnalysisHighlightCategory.TyreTempImbalance in categories)
        assertTrue(SessionAnalysisHighlightCategory.TyrePressureImbalance in categories)
        assertTrue(SessionAnalysisHighlightCategory.AeroBalance in categories)
        assertEquals(
            SessionAnalysisDiagnosisSource.CarSetup,
            report.cornerInsights[SessionAnalysisCornerKey(segmentId = 1L, cornerNumber = 5)]?.diagnosisSource,
        )
        assertContains(setupUndersteer.recommendation, "one step first")
        assertContains(pressure.recommendation, "0.3-0.5 psi")
    }
}

private fun buildSetupCorner(
    lapNumber: Int,
    apexTrackPosition: Float,
    timeLossMs: Int,
): SessionAnalysisCornerAnalysis = SessionAnalysisCornerAnalysis(
    segmentId = 1L,
    lapNumber = lapNumber,
    cornerNumber = 5,
    score = 54,
    startTrackPosition = apexTrackPosition - 0.05f,
    apexTrackPosition = apexTrackPosition,
    endTrackPosition = apexTrackPosition + 0.06f,
    understeerRatio = 0.52f,
    oversteerRatio = 0.05f,
    entrySpeedKmh = 168f,
    apexSpeedKmh = 103f,
    exitSpeedKmh = 126f,
    timeLossMs = timeLossMs,
    representativeSample = SessionAnalysisSample(
        segmentId = 1L,
        lapNumber = lapNumber,
        sampleIndexInLap = 42,
        trackPosition = apexTrackPosition,
        trackX = 120f + lapNumber,
        trackY = 60f + lapNumber,
        deltaToBestMs = timeLossMs,
    ),
)

private fun buildSetupSample(
    lapNumber: Int,
    trackPosition: Float,
): SessionAnalysisSample {
    val hotFrontTyre = SessionAnalysisTyreState(
        pressurePsi = 28.4f,
        coreTempC = 103f,
        innerTempC = 106f,
        middleTempC = 97f,
        outerTempC = 86f,
        brakeTempC = 520f,
    )
    val rearTyre = SessionAnalysisTyreState(
        pressurePsi = 26.5f,
        coreTempC = 94f,
        innerTempC = 95f,
        middleTempC = 91f,
        outerTempC = 87f,
        brakeTempC = 410f,
    )
    return SessionAnalysisSample(
        segmentId = 1L,
        lapNumber = lapNumber,
        sampleIndexInLap = 42,
        trackPosition = trackPosition,
        speedKmh = 162f,
        deltaToBestMs = 90 + lapNumber * 5,
        tyreFl = hotFrontTyre,
        tyreFr = hotFrontTyre.copy(pressurePsi = 28.1f),
        tyreRl = rearTyre,
        tyreRr = rearTyre.copy(pressurePsi = 26.3f),
    )
}

private fun gt3TyreProfile(): SessionAnalysisTyreProfile = SessionAnalysisTyreProfile(
    vehicleClass = SessionAnalysisVehicleClass.GT3,
    compoundFamily = SessionAnalysisTyreCompoundFamily.Medium,
    surfaceOptimalMinC = 75f,
    surfaceOptimalMaxC = 95f,
    coreOptimalMinC = 80f,
    coreOptimalMaxC = 100f,
    brakeOptimalMinC = 350f,
    brakeOptimalMaxC = 520f,
    pressureOptimalMinPsi = 26.0f,
    pressureOptimalMaxPsi = 27.0f,
    innerOuterSpreadWarnC = 6f,
    innerOuterSpreadCriticalC = 10f,
)

