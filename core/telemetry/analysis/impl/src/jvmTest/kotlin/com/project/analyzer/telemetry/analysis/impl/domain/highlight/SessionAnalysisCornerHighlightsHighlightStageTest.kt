package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisConsistencyReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisCornerConsistency
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerApexClassification
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisCornerSetupInsight
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnostic
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnosticReport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionAnalysisCornerHighlightsHighlightStageTest {

    private val stage = SessionAnalysisCornerHighlightsHighlightStage()

    @Test
    fun `execute promotes corner issues and setup diagnostics into drafts`() = runBlocking {
        val result = stage.execute(
            SessionAnalysisHighlightContext(
                samples = listOf(highlightSample(lapNumber = 2, sampleIndexInLap = 5, trackPosition = 0.18f)),
                bestLapBySegmentId = mapOf(1L to 1),
                tyreProfile = null,
                cornerReport = SessionAnalysisCornerAnalysisReport(
                    corners = listOf(
                        SessionAnalysisCornerAnalysis(
                            segmentId = 1L,
                            lapNumber = 2,
                            cornerNumber = 3,
                            score = 58,
                            representativeSample = highlightSample(
                                lapNumber = 2,
                                sampleIndexInLap = 5,
                                trackPosition = 0.18f,
                                deltaToBestMs = 128,
                            ),
                            startTrackPosition = 0.08f,
                            apexTrackPosition = 0.18f,
                            endTrackPosition = 0.30f,
                            brakePointTrackPosition = 0.11f,
                            throttlePickupTrackPosition = 0.24f,
                            coastingRatio = 0.28f,
                            trailBrakingScore = 42,
                            apexClassification = SessionAnalysisCornerApexClassification.EarlyApex,
                            timeLossMs = 128,
                            understeerRatio = 0.46f,
                            wheelLockup = true,
                        ),
                    ),
                ),
                setupReport = SessionAnalysisSetupDiagnosticReport(
                    diagnostics = listOf(
                        SessionAnalysisSetupDiagnostic(
                            category = SessionAnalysisHighlightCategory.SetupUndersteer,
                            severity = SessionAnalysisHighlightSeverity.Warning,
                            title = "Front end washes wide",
                            description = "The front axle is overloaded through the loaded phase.",
                            recommendation = "Soften the front anti-roll bar.",
                            diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                            segmentId = 1L,
                            lapNumber = 2,
                            cornerNumber = 3,
                        ),
                    ),
                    cornerInsights = mapOf(
                        SessionAnalysisCornerKey(segmentId = 1L, cornerNumber = 3) to SessionAnalysisCornerSetupInsight(
                            diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                            recommendation = "Soften the front anti-roll bar.",
                        ),
                    ),
                ),
                consistencyReport = SessionAnalysisConsistencyReport(
                    overallScore = 74,
                    corners = listOf(
                        SessionAnalysisCornerConsistency(
                            segmentId = 1L,
                            cornerNumber = 3,
                            trackPosition = 0.18f,
                            score = 64,
                            affectedLaps = listOf(1, 2),
                        ),
                    ),
                ),
            ),
        )

        val categories = result.drafts.map(SessionAnalysisHighlightDraft::category).toSet()
        val trailBrakeDraft = result.drafts.first { draft ->
            draft.category == SessionAnalysisHighlightCategory.TrailBrakingMissing
        }
        val understeerDraft = result.drafts.first { draft ->
            draft.category == SessionAnalysisHighlightCategory.Understeer
        }

        assertTrue(SessionAnalysisHighlightCategory.TimeLoss in categories)
        assertTrue(SessionAnalysisHighlightCategory.TrailBrakingMissing in categories)
        assertTrue(SessionAnalysisHighlightCategory.EarlyApexEntry in categories)
        assertTrue(SessionAnalysisHighlightCategory.WheelLockup in categories)
        assertTrue(SessionAnalysisHighlightCategory.Understeer in categories)
        assertTrue(SessionAnalysisHighlightCategory.InconsistentLine in categories)
        assertTrue(SessionAnalysisHighlightCategory.SetupUndersteer in categories)
        assertContains(trailBrakeDraft.recommendation, "brake pressure to apex")
        assertContains(understeerDraft.recommendation, "front anti-roll bar")
    }

    @Test
    fun `execute leaves context unchanged when diagnostics are unavailable`() = runBlocking {
        val input = highlightContext(samples = listOf(highlightSample()))

        val result = stage.execute(input)

        assertEquals(input, result)
    }
}
