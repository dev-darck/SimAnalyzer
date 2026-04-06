package com.analyzer.session.analysis.presentation.components.inspector.support

import com.analyzer.session.analysis.presentation.model.SessionAnalysisCoachInsightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisCoachTone
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapCoachUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisInspectorNarrativeSupportTest {

    @Test
    fun `resolve line narratives groups highlights by execution theme`() {
        val lapCoach = SessionAnalysisLapCoachUi(
            referenceLapLabel = "Ref lap",
            summaryTitle = "Focus",
            summaryDescription = "Summary",
            insights = persistentListOf(
                SessionAnalysisCoachInsightUi(
                    title = "Generic coach item",
                    description = "This should not win over line highlights.",
                    tone = SessionAnalysisCoachTone.Warning,
                ),
            ),
        )
        val highlights = listOf(
            lineHighlight(
                category = SessionAnalysisHighlightCategory.TrailBrakingMissing,
                title = "Trail braking fades too early",
                recommendation = "Carry brake pressure closer to the apex.",
                priority = 9,
                deltaMs = 150,
                cornerNumber = 2,
            ),
            lineHighlight(
                category = SessionAnalysisHighlightCategory.BrakePoint,
                title = "Brake marker drift",
                recommendation = "Tighten the brake release.",
                priority = 6,
                deltaMs = 60,
                cornerNumber = 4,
            ),
            lineHighlight(
                category = SessionAnalysisHighlightCategory.EarlyApexEntry,
                title = "Early apex",
                recommendation = "Delay turn-in slightly to open the exit.",
                priority = 8,
                deltaMs = 120,
                cornerNumber = 6,
            ),
            lineHighlight(
                category = SessionAnalysisHighlightCategory.WheelSpin,
                title = "Wheel spin",
                recommendation = "Open the steering sooner before full throttle.",
                priority = 7,
                deltaMs = 95,
                cornerNumber = 8,
            ),
        )

        val narratives = resolveLineNarratives(
            diagnosticSummary = null,
            lapCoach = lapCoach,
            highlights = highlights,
        )

        assertEquals(3, narratives.size)
        assertEquals("Turn 2: Entry braking is costing time", narratives[0].title)
        assertEquals("Turn 6: Mid-corner line is costing time", narratives[1].title)
        assertEquals("Turn 8: Exit throttle timing is costing time", narratives[2].title)
        assertTrue(narratives[0].description.contains("turn 2"))
        assertTrue(narratives[0].description.contains("150 ms"))
        assertEquals("Carry brake pressure closer to the apex.", narratives[0].recommendation)
        assertEquals("Turn 2: entry brake trace, release timing and minimum speed", narratives[0].lookAt)
        assertEquals(SessionAnalysisDiagnosisSource.DrivingStyle, narratives[0].source)
    }

    @Test
    fun `resolve line narratives falls back to coach insights when highlights are not line relevant`() {
        val lapCoach = SessionAnalysisLapCoachUi(
            referenceLapLabel = "Ref lap",
            summaryTitle = "Focus",
            summaryDescription = "Summary",
            insights = persistentListOf(
                SessionAnalysisCoachInsightUi(
                    title = "Brake marker trend",
                    description = "Brake 1.2% lap later than reference.",
                    tone = SessionAnalysisCoachTone.Warning,
                ),
            ),
        )

        val narratives = resolveLineNarratives(
            diagnosticSummary = null,
            lapCoach = lapCoach,
            highlights = listOf(
                lineHighlight(
                    category = SessionAnalysisHighlightCategory.TyrePressureImbalance,
                    title = "Pressure split",
                    recommendation = "Adjust pressures.",
                    priority = 8,
                    deltaMs = 110,
                ),
            ),
        )

        assertEquals(1, narratives.size)
        assertEquals("Brake marker trend", narratives.first().title)
        assertEquals("Reference trace and input timing", narratives.first().lookAt)
    }

    @Test
    fun `resolve line narratives drops generic time loss when same corner has a specific coaching issue`() {
        val narratives = resolveLineNarratives(
            diagnosticSummary = null,
            lapCoach = null,
            highlights = listOf(
                lineHighlight(
                    category = SessionAnalysisHighlightCategory.TimeLoss,
                    title = "Corner 4 is a repeatable loss point",
                    recommendation = "Open the reference trace here first.",
                    priority = 10,
                    deltaMs = 240,
                    cornerNumber = 4,
                ),
                lineHighlight(
                    category = SessionAnalysisHighlightCategory.TrailBrakingMissing,
                    title = "Trail braking fades too early in corner 4",
                    recommendation = "Carry brake pressure closer to the apex.",
                    priority = 7,
                    deltaMs = 120,
                    cornerNumber = 4,
                ),
            ),
        )

        assertEquals(1, narratives.size)
        assertEquals("Turn 4: Entry braking is costing time", narratives.first().title)
        assertEquals("Carry brake pressure closer to the apex.", narratives.first().recommendation)
        assertEquals("Turn 4: entry brake trace, release timing and minimum speed", narratives.first().lookAt)
    }

    @Test
    fun `resolve line narratives prefers aggregated driving issues when available`() {
        val narratives = resolveLineNarratives(
            diagnosticSummary = SessionAnalysisDiagnosticSummaryUi(
                topDrivingIssues = persistentListOf(
                    com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi(
                        title = "Turn 7 entry phase is costing time",
                        description = "Brake release fades too early before the apex.",
                        recommendation = "Carry a small amount of brake closer to apex.",
                        priority = 10,
                        source = SessionAnalysisDiagnosisSource.DrivingStyle,
                        potentialTimeGainMs = 210,
                        category = SessionAnalysisHighlightCategory.TrailBrakingMissing,
                        cornerNumber = 7,
                    ),
                ),
            ),
            lapCoach = SessionAnalysisLapCoachUi(
                referenceLapLabel = "Ref lap",
                summaryTitle = "Focus",
                summaryDescription = "Summary",
                insights = persistentListOf(),
            ),
            highlights = emptyList(),
        )

        assertEquals(1, narratives.size)
        assertEquals("Turn 7 entry phase is costing time", narratives.first().title)
        assertTrue(narratives.first().description.contains("210 ms"))
        assertEquals("Carry a small amount of brake closer to apex.", narratives.first().recommendation)
        assertEquals("Turn 7: entry brake trace, release timing and minimum speed", narratives.first().lookAt)
    }
}

private fun lineHighlight(
    category: SessionAnalysisHighlightCategory,
    title: String,
    recommendation: String,
    priority: Int,
    deltaMs: Int,
    cornerNumber: Int? = null,
): SessionAnalysisHighlightUi = SessionAnalysisHighlightUi(
    category = category,
    severity = SessionAnalysisHighlightSeverity.Warning,
    lapNumber = 4,
    title = title,
    description = title,
    trackPosition = 0.25f,
    deltaMs = deltaMs,
    diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
    recommendation = recommendation,
    cornerNumber = cornerNumber,
    priority = priority,
)
