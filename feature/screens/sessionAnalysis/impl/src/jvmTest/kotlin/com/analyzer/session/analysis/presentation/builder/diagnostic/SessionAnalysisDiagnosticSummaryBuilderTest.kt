@file:Suppress(
    "LongMethod",
    "LongParameterList",
)

package com.analyzer.session.analysis.presentation.builder.diagnostic

import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapSummaryUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisDiagnosticSummaryBuilderTest {

    @Test
    fun `build summary separates driving and setup issues and applies lap scores`() {
        val highlights = listOf(
            highlight(
                id = "drive-loss",
                category = SessionAnalysisHighlightCategory.TimeLoss,
                title = "Turn 1 braking loss",
                diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                priority = 10,
                deltaMs = 220,
                cornerNumber = 1,
                score = 62,
            ),
            highlight(
                id = "setup-understeer",
                category = SessionAnalysisHighlightCategory.SetupUndersteer,
                title = "Turn 5 chronic understeer",
                diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                priority = 9,
                deltaMs = 180,
                cornerNumber = 5,
                score = 55,
                recommendation = "Soften the front anti-roll bar or add front wing.",
            ),
            highlight(
                id = "pressure",
                category = SessionAnalysisHighlightCategory.TyrePressureImbalance,
                title = "Baseline pressure split",
                diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                priority = 8,
                deltaMs = 120,
                affectedLaps = persistentListOf(2, 3),
                recommendation = "Trim front pressures down by 0.5 psi.",
            ),
        )

        val summary = buildDiagnosticSummary(highlights)
        val lapScores = applyLapDiagnosticScores(
            laps = listOf(
                SessionAnalysisLapSummaryUi(
                    lapNumber = 2,
                    isValid = true,
                    isPitLap = false,
                    isComplete = true,
                    sampleCount = 200,
                ),
                SessionAnalysisLapSummaryUi(
                    lapNumber = 4,
                    isValid = true,
                    isPitLap = false,
                    isComplete = true,
                    sampleCount = 200,
                ),
            ),
            highlights = highlights,
        )

        assertNotNull(summary)
        requireNotNull(summary)
        assertEquals("Turn 1 is losing time versus reference", summary.topDrivingIssues.first().title)
        assertEquals("Car pushes wide in turn 5", summary.topSetupIssues.first().title)
        assertEquals("Baseline pressure split", summary.topSetupIssues[1].title)
        assertTrue(summary.topSetupIssues.first().description.contains("Peak loss is 180 ms"))
        assertEquals(2, summary.cornerScores.size)
        assertTrue(summary.overallScore > 0)
        assertTrue(summary.drivingScore > 0)
        assertTrue(summary.setupScore > 0)
        assertTrue(summary.overallScore < 100)
        assertTrue(summary.drivingScore < 100)
        assertTrue(summary.setupScore < 100)
        assertTrue((lapScores.first().diagnosticScore ?: 100) < 100)
        assertEquals(100, lapScores.last().diagnosticScore)
    }

    @Test
    fun `build summary promotes root cause over generic time loss in the same corner`() {
        val highlights = listOf(
            highlight(
                id = "corner-3-loss",
                category = SessionAnalysisHighlightCategory.TimeLoss,
                title = "Corner 3 costs time",
                diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                priority = 10,
                deltaMs = 240,
                cornerNumber = 3,
                score = 58,
                recommendation = "Start review here.",
            ),
            highlight(
                id = "corner-3-trail",
                category = SessionAnalysisHighlightCategory.TrailBrakingMissing,
                title = "Trail braking fades too early in corner 3",
                diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                priority = 6,
                deltaMs = 120,
                cornerNumber = 3,
                score = 58,
                recommendation = "Carry a small amount of brake closer to the apex.",
            ),
        )

        val summary = buildDiagnosticSummary(highlights)

        assertNotNull(summary)
        requireNotNull(summary)
        assertEquals("Turn 3: brake release ends too early", summary.topDrivingIssues.first().title)
        assertEquals(240, summary.topDrivingIssues.first().potentialTimeGainMs)
        assertEquals("Turn 3: brake release ends too early", summary.cornerScores.first().mainIssue)
        assertTrue(summary.cornerScores.first().detail.contains("Peak loss is 240 ms"))
        assertEquals("Carry a small amount of brake closer to the apex.", summary.cornerScores.first().recommendation)
    }

    @Test
    fun `build summary gives generic time loss a driving-focused title`() {
        val summary = buildDiagnosticSummary(
            listOf(
                highlight(
                    id = "corner-4-loss",
                    category = SessionAnalysisHighlightCategory.TimeLoss,
                    title = "Corner 4 is costing time",
                    description = "Corner 4 gives away 210 ms versus the benchmark lap. The loss starts on entry where the car arrives about 6 km/h slower than the reference.",
                    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
                    priority = 10,
                    deltaMs = 210,
                    cornerNumber = 4,
                    recommendation = "Brake a touch later and keep the front loaded instead of overslowing the entry.",
                ),
            ),
        )

        requireNotNull(summary)
        assertEquals("Turn 4: entry braking is costing time", summary.topDrivingIssues.first().title)
        assertEquals("Turn 4: entry braking is costing time", summary.cornerScores.first().mainIssue)
    }

    @Test
    fun `build summary merges repeated setup balance issues across multiple corners`() {
        val highlights = listOf(
            highlight(
                id = "setup-understeer-3",
                category = SessionAnalysisHighlightCategory.SetupUndersteer,
                title = "Chronic understeer in corner 3",
                diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                priority = 8,
                deltaMs = 110,
                cornerNumber = 3,
                recommendation = "Soften the front anti-roll bar or add front downforce.",
                affectedLaps = persistentListOf(2, 3, 4),
            ),
            highlight(
                id = "setup-understeer-7",
                category = SessionAnalysisHighlightCategory.SetupUndersteer,
                title = "Chronic understeer in corner 7",
                diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                priority = 9,
                deltaMs = 170,
                cornerNumber = 7,
                recommendation = "Soften the front anti-roll bar or add front downforce.",
                affectedLaps = persistentListOf(2, 3, 4),
            ),
        )

        val summary = buildDiagnosticSummary(highlights)

        assertNotNull(summary)
        requireNotNull(summary)
        assertEquals(1, summary.topSetupIssues.size)
        assertEquals("Car pushes wide in turns 3 and 7", summary.topSetupIssues.first().title)
        assertTrue(summary.topSetupIssues.first().description.contains("Turn 3 and 7").not())
        assertTrue(summary.topSetupIssues.first().description.contains("Turns 3 and 7"))
        assertTrue(summary.topSetupIssues.first().description.contains("Seen across 3 laps"))
    }

    @Test
    fun `build summary keeps specific setup title for aero and brake issues`() {
        val summary = buildDiagnosticSummary(
            listOf(
                highlight(
                    id = "aero-rear",
                    category = SessionAnalysisHighlightCategory.AeroBalance,
                    title = "High-speed rear instability",
                    diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                    priority = 8,
                    deltaMs = 140,
                    recommendation = "Add rear wing first.",
                ),
                highlight(
                    id = "brake-bias",
                    category = SessionAnalysisHighlightCategory.BrakeBalance,
                    title = "Front axle lockup under straight-line braking",
                    diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
                    priority = 7,
                    deltaMs = 110,
                    recommendation = "Move bias rearward first.",
                ),
            ),
        )

        requireNotNull(summary)
        assertEquals("High-speed rear instability", summary.topSetupIssues.first().title)
        assertEquals("Front axle lockup under straight-line braking", summary.topSetupIssues[1].title)
    }
}

