package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.header.SessionAnalysisHeader
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreCompoundFamily
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisConsistencyReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisCornerConsistency
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerApexClassification
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerReference
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisCornerSetupInsight
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnostic
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnosticReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComprehensiveSessionAnalysisResolverTest {

    private val resolver = ComprehensiveSessionAnalysisResolver(
        consistencyBuilder = ComprehensiveSessionAnalysisConsistencyBuilder(),
        contextResolver = ComprehensiveSessionAnalysisContextResolver(),
        cornerBuilder = ComprehensiveSessionAnalysisCornerBuilder(),
        segmentBuilder = ComprehensiveSessionAnalysisSegmentBuilder(),
        summaryBuilder = ComprehensiveSessionAnalysisSummaryBuilder(),
        tyreFuelBuilder = ComprehensiveSessionAnalysisTyreFuelBuilder(),
    )

    @Test
    fun `resolve builds context summary and setup recommendations`() = kotlinx.coroutines.runBlocking {
        val report = report()
        val cornerReport = cornerReport()
        val setupReport = setupReport()
        val consistencyReport = consistencyReport()
        val highlights = listOf(
            SessionAnalysisHighlight(
                id = "top-speed",
                category = SessionAnalysisHighlightCategory.TopSpeed,
                severity = SessionAnalysisHighlightSeverity.Positive,
                segmentId = 1L,
                lapNumber = 1,
                title = "Top speed",
                description = "Fast on the straight",
                diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                recommendation = "Keep this exit",
            ),
        )

        val analysis = resolver.resolve(
            report = report,
            cornerReport = cornerReport,
            setupReport = setupReport,
            consistencyReport = consistencyReport,
            highlights = highlights,
        )

        assertEquals(SessionAnalysisVehicleClass.GT3, analysis.context.vehicleClass)
        assertEquals("Porsche 911 GT3 R", analysis.context.vehicleName)
        assertEquals("Spa", analysis.context.trackName)
        assertEquals("Qualifying", analysis.context.sessionType.name)
        assertEquals(2, analysis.sessionSummary.validLaps)
        assertFalse(analysis.segmentAnalyses.isEmpty())
        assertFalse(analysis.cornerAnalyses.isEmpty())
        assertEquals(SessionAnalysisHighlightCategory.SetupUndersteer, setupReport.diagnostics.first().category)
        assertEquals(1, analysis.setupRecommendations.size)
        assertTrue(analysis.setupRecommendations.first().suggestedChange.contains("front anti-roll bar"))
        assertTrue(analysis.cornerAnalyses.first().primaryIssue != null)
        assertTrue(analysis.sessionSummary.top3ImprovementAreas.isNotEmpty())
        assertNotNull(analysis.comparativeAnalysis)
        assertTrue(analysis.narrative.contains("Spa"))
    }

    @Test
    fun `resolve tolerates missing optional reports`() = kotlinx.coroutines.runBlocking {
        val analysis = resolver.resolve(
            report = report(),
            cornerReport = null,
            setupReport = null,
            consistencyReport = null,
            highlights = emptyList(),
        )

        assertTrue(analysis.cornerAnalyses.isEmpty())
        assertTrue(analysis.segmentAnalyses.isEmpty())
        assertTrue(analysis.setupRecommendations.isEmpty())
        assertEquals(2, analysis.sessionSummary.validLaps)
        assertNotNull(analysis.comparativeAnalysis)
    }

    @Test
    fun `resolve returns empty branches for empty session data`() = kotlinx.coroutines.runBlocking {
        val analysis = resolver.resolve(
            report = report().copy(
                tyreProfile = null,
                segments = emptyList(),
                laps = emptyList(),
                samples = emptyList(),
            ),
            cornerReport = null,
            setupReport = null,
            consistencyReport = null,
            highlights = emptyList(),
        )

        assertEquals(0, analysis.sessionSummary.validLaps)
        assertTrue(analysis.segmentAnalyses.isEmpty())
        assertTrue(analysis.cornerAnalyses.isEmpty())
        assertTrue(analysis.setupRecommendations.isEmpty())
        assertEquals(null, analysis.comparativeAnalysis)
    }

    @Test
    fun `resolve omits comparative analysis for single lap sessions`() = kotlinx.coroutines.runBlocking {
        val singleLapReport = report().copy(
            laps = report().laps.take(1),
            samples = lapSamples(1, 0),
        )

        val analysis = resolver.resolve(
            report = singleLapReport,
            cornerReport = cornerReport().copy(corners = cornerReport().corners.take(1)),
            setupReport = null,
            consistencyReport = null,
            highlights = emptyList(),
        )

        assertEquals(1, analysis.sessionSummary.validLaps)
        assertEquals(null, analysis.comparativeAnalysis)
    }

    @Test
    fun `resolve merges repeated setup diagnostics into one recommendation`() = kotlinx.coroutines.runBlocking {
        val analysis = resolver.resolve(
            report = report(),
            cornerReport = cornerReport(),
            setupReport = SessionAnalysisSetupDiagnosticReport(
                diagnostics = listOf(
                    SessionAnalysisSetupDiagnostic(
                        category = SessionAnalysisHighlightCategory.SetupUndersteer,
                        severity = SessionAnalysisHighlightSeverity.Warning,
                        title = "Chronic understeer in corner 3",
                        description = "Understeer repeats on 3 analysed laps through the same corner.",
                        recommendation = "Soften the front anti-roll bar or add front downforce.",
                        diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                        segmentId = 1L,
                        lapNumber = 2,
                        cornerNumber = 3,
                        affectedLaps = listOf(1, 2, 3),
                    ),
                    SessionAnalysisSetupDiagnostic(
                        category = SessionAnalysisHighlightCategory.SetupUndersteer,
                        severity = SessionAnalysisHighlightSeverity.Warning,
                        title = "Chronic understeer in corner 7",
                        description = "Understeer repeats on 3 analysed laps through the same corner.",
                        recommendation = "Soften the front anti-roll bar or add front downforce.",
                        diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                        segmentId = 1L,
                        lapNumber = 2,
                        cornerNumber = 7,
                        affectedLaps = listOf(1, 2, 3),
                    ),
                ),
                cornerInsights = emptyMap(),
            ),
            consistencyReport = consistencyReport(),
            highlights = emptyList(),
        )

        assertEquals(1, analysis.setupRecommendations.size)
        assertEquals(listOf(3, 7), analysis.setupRecommendations.first().affectedCorners)
        assertTrue(analysis.setupRecommendations.first().reason.contains("Turns 3 and 7"))
        assertTrue(analysis.setupRecommendations.first().reason.contains("Seen across 3 laps"))
    }

    @Test
    fun `resolve keeps corner issue neutral when corner is not slower than reference`() =
        kotlinx.coroutines.runBlocking {
            val analysis = resolver.resolve(
                report = report(),
                cornerReport = SessionAnalysisCornerAnalysisReport(
                    corners = listOf(
                        cornerAnalysis(
                            lapNumber = 2,
                            deltaMs = -40,
                            brakePoint = 0.10f,
                            apexSpeed = 109f,
                            throttlePickup = 0.25f,
                            understeer = 0.18f,
                        ),
                    ),
                    referenceCornersBySegmentId = cornerReport().referenceCornersBySegmentId,
                ),
                setupReport = null,
                consistencyReport = null,
                highlights = emptyList(),
            )

            assertEquals(1, analysis.cornerAnalyses.size)
            assertEquals(
                com.project.analyzer.telemetry.analysis.api.model.report.corner.CornerIssue.NONE,
                analysis.cornerAnalyses.first().primaryIssue
            )
        }
}

private fun report(): SessionAnalysisReport = SessionAnalysisReport(
    sessionId = 1L,
    header = SessionAnalysisHeader(
        gameId = "acevo",
        sessionTypeLabel = "Qualifying",
        carLabel = "Porsche 911 GT3 R",
        trackLabel = "Spa",
        startedAtMs = 1_000L,
    ),
    vehicleClass = SessionAnalysisVehicleClass.GT3,
    tyreProfile = SessionAnalysisTyreProfile(
        vehicleClass = SessionAnalysisVehicleClass.GT3,
        compoundFamily = SessionAnalysisTyreCompoundFamily.Medium,
        compoundLabel = "Medium",
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
    ),
    segments = listOf(
        SessionAnalysisSegment(
            segmentId = 1L,
            sessionTypeLabel = "Qualifying",
            sessionLabel = "Qualifying",
            carLabel = "Porsche 911 GT3 R",
            trackLabel = "Spa",
            startedAtMs = 1_000L,
            endedAtMs = 101_000L,
        ),
    ),
    laps = listOf(
        SessionAnalysisLap(
            segmentId = 1L,
            sessionTypeLabel = "Qualifying",
            lapNumber = 1,
            isValid = true,
            isPitLap = false,
            isComplete = true,
            durationMs = 90_000,
            fuelUsedLiters = 2.5f
        ),
        SessionAnalysisLap(
            segmentId = 1L,
            sessionTypeLabel = "Qualifying",
            lapNumber = 2,
            isValid = true,
            isPitLap = false,
            isComplete = true,
            durationMs = 90_550,
            deltaToBestMs = 550,
            fuelUsedLiters = 2.7f
        ),
    ),
    samples = lapSamples(1, 0) + lapSamples(2, 550),
)

private fun cornerReport(): SessionAnalysisCornerAnalysisReport = SessionAnalysisCornerAnalysisReport(
    corners = listOf(
        cornerAnalysis(
            lapNumber = 1,
            deltaMs = 0,
            brakePoint = 0.10f,
            apexSpeed = 108f,
            throttlePickup = 0.25f,
            understeer = 0.12f
        ),
        cornerAnalysis(
            lapNumber = 2,
            deltaMs = 90,
            brakePoint = 0.08f,
            apexSpeed = 100f,
            throttlePickup = 0.28f,
            understeer = 0.34f
        ),
    ),
    referenceCornersBySegmentId = mapOf(
        1L to listOf(
            SessionAnalysisCornerReference(
                cornerNumber = 1,
                startTrackPosition = 0.06f,
                apexTrackPosition = 0.18f,
                endTrackPosition = 0.30f,
                brakePointTrackPosition = 0.10f,
                throttlePickupTrackPosition = 0.25f,
                entrySpeedKmh = 176f,
                apexSpeedKmh = 108f,
                exitSpeedKmh = 142f,
            ),
        ),
    ),
)

private fun setupReport(): SessionAnalysisSetupDiagnosticReport = SessionAnalysisSetupDiagnosticReport(
    diagnostics = listOf(
        SessionAnalysisSetupDiagnostic(
            category = SessionAnalysisHighlightCategory.SetupUndersteer,
            severity = SessionAnalysisHighlightSeverity.Warning,
            title = "Chronic understeer",
            description = "Front axle washes wide through the loaded phase.",
            recommendation = "Soften the front anti-roll bar or add front downforce.",
            diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
            segmentId = 1L,
            lapNumber = 2,
            cornerNumber = 1,
            affectedLaps = listOf(1, 2),
        ),
    ),
    cornerInsights = mapOf(
        SessionAnalysisCornerKey(1L, 1) to SessionAnalysisCornerSetupInsight(
            diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
            recommendation = "Soften the front anti-roll bar or add front downforce.",
        ),
    ),
)

private fun consistencyReport(): SessionAnalysisConsistencyReport = SessionAnalysisConsistencyReport(
    overallScore = 74,
    corners = listOf(
        SessionAnalysisCornerConsistency(
            segmentId = 1L,
            cornerNumber = 1,
            trackPosition = 0.18f,
            score = 68,
            apexSpeedStdKmh = 4.6f,
            affectedLaps = listOf(1, 2),
        ),
    ),
)

private fun cornerAnalysis(
    lapNumber: Int,
    deltaMs: Int,
    brakePoint: Float,
    apexSpeed: Float,
    throttlePickup: Float,
    understeer: Float,
): SessionAnalysisCornerAnalysis {
    val samples = lapSamples(lapNumber, deltaMs)
    return SessionAnalysisCornerAnalysis(
        segmentId = 1L,
        lapNumber = lapNumber,
        cornerNumber = 1,
        score = if (lapNumber == 1) 88 else 63,
        samples = samples,
        representativeSample = samples[2],
        startTrackPosition = 0.06f,
        apexTrackPosition = 0.18f,
        endTrackPosition = 0.30f,
        brakePointTrackPosition = brakePoint,
        throttlePickupTrackPosition = throttlePickup,
        coastingRatio = if (lapNumber == 1) 0.12f else 0.24f,
        trailBrakingScore = if (lapNumber == 1) 81 else 58,
        apexClassification = if (lapNumber == 1) SessionAnalysisCornerApexClassification.GoodApex else SessionAnalysisCornerApexClassification.EarlyApex,
        entrySpeedKmh = 176f,
        apexSpeedKmh = apexSpeed,
        exitSpeedKmh = if (lapNumber == 1) 142f else 136f,
        timeLossMs = deltaMs,
        understeerRatio = understeer,
        oversteerRatio = 0.08f,
        wheelLockup = lapNumber == 2,
        wheelSpin = false,
        referenceApexTrackPosition = 0.18f,
    )
}

private fun lapSamples(lapNumber: Int, deltaMs: Int): List<SessionAnalysisSample> = listOf(
    sample(
        lapNumber,
        0,
        0.06f,
        176f,
        brake = 0.92f,
        throttle = 0.05f,
        fuel = 40f - lapNumber,
        slipFront = if (lapNumber == 2) 0.22f else 0.10f
    ),
    sample(
        lapNumber,
        1,
        0.12f,
        138f,
        brake = 0.58f,
        throttle = 0.02f,
        fuel = 40f - lapNumber,
        slipFront = if (lapNumber == 2) 0.18f else 0.08f
    ),
    sample(
        lapNumber,
        2,
        0.18f,
        if (lapNumber == 1) 108f else 100f,
        brake = 0.12f,
        throttle = 0.08f,
        fuel = 40f - lapNumber,
        deltaMs = deltaMs
    ),
    sample(
        lapNumber,
        3,
        0.24f,
        if (lapNumber == 1) 126f else 120f,
        brake = 0.02f,
        throttle = if (lapNumber == 1) 0.46f else 0.28f,
        fuel = 39.5f - lapNumber
    ),
    sample(
        lapNumber,
        4,
        0.30f,
        if (lapNumber == 1) 142f else 136f,
        brake = 0f,
        throttle = 0.92f,
        fuel = 39f - lapNumber
    ),
)

private fun sample(
    lapNumber: Int,
    index: Int,
    trackPosition: Float,
    speed: Float,
    brake: Float,
    throttle: Float,
    fuel: Float,
    deltaMs: Int = 0,
    slipFront: Float = 0.08f,
): SessionAnalysisSample = SessionAnalysisSample(
    segmentId = 1L,
    frameId = lapNumber * 100L + index,
    timestampNs = lapNumber * 1_000_000_000L + index * 20_000_000L,
    lapNumber = lapNumber,
    sampleIndexInLap = index,
    trackPosition = trackPosition,
    trackX = trackPosition * 1_000f,
    trackY = trackPosition * 40f,
    speedKmh = speed,
    throttle = throttle,
    brake = brake,
    steeringAngleRad = 0.12f - index * 0.02f,
    lateralG = 1.0f + index * 0.05f,
    yawRateRad = 0.22f,
    deltaToBestMs = deltaMs,
    fuelLiters = fuel,
    tyreFl = SessionAnalysisTyreState(coreTempC = 96f, pressurePsi = 27.4f, slip = slipFront),
    tyreFr = SessionAnalysisTyreState(coreTempC = 97f, pressurePsi = 27.1f, slip = slipFront),
    tyreRl = SessionAnalysisTyreState(coreTempC = 93f, pressurePsi = 26.6f, slip = 0.09f),
    tyreRr = SessionAnalysisTyreState(coreTempC = 93f, pressurePsi = 26.5f, slip = 0.09f),
)
