package com.analyzer.session.analysis.presentation.builder.track

import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackMapUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackPointUi
import com.analyzer.session.analysis.presentation.model.toUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionAnalysisTrackCanvasStateFactoryTest {

    private val factory = SessionAnalysisTrackCanvasStateFactory

    @Test
    fun `build uses geometry corner zones for map markers and keeps neutral corners`() {
        val trackMapUi = sessionTrackMapUi()
        val sourceTrackMap = sessionTrackMap()
        val diagnosticSummary = SessionAnalysisDiagnosticSummaryUi(
            cornerScores = persistentListOf(
                CornerScoreUi(
                    cornerNumber = 17,
                    score = 74,
                    trackPosition = 0.42f,
                    mainIssue = "Corner 2 costs time",
                    timeVsReferenceMs = 118,
                    recommendation = "Open the exit earlier.",
                    source = SessionAnalysisDiagnosisSource.DrivingStyle.toUi(),
                ),
            ),
        )

        val state = factory.build(
            input = SessionAnalysisTrackCanvasStateInput(
                trackMap = trackMapUi,
                authoredTrackMap = null,
                sourceTrackMap = sourceTrackMap,
                displayTrackMap = sourceTrackMap,
                visibleSamplesCore = emptyList(),
                referenceSamplesCore = emptyList(),
                sectors = persistentListOf(),
                highlights = persistentListOf(),
                diagnosticSummary = diagnosticSummary,
                cornerZones = listOf(
                    SessionAnalysisCornerZone(
                        cornerNumber = 4,
                        startTrackPosition = 0.08f,
                        endTrackPosition = 0.16f,
                        apexTrackPosition = 0.12f,
                        peakCurvature = 0.021f,
                    ),
                    SessionAnalysisCornerZone(
                        cornerNumber = 17,
                        startTrackPosition = 0.38f,
                        endTrackPosition = 0.50f,
                        apexTrackPosition = 0.44f,
                        peakCurvature = 0.033f,
                    ),
                ),
            ),
        )

        assertNotNull(state)
        assertEquals(listOf(4, 17), state.cornerMarkers.map(CornerScoreUi::cornerNumber))
        assertEquals(100, state.cornerMarkers[0].score)
        assertEquals(0.12f, state.cornerMarkers[0].trackPosition)
        assertEquals(74, state.cornerMarkers[1].score)
        assertEquals(0.44f, state.cornerMarkers[1].trackPosition)
        assertEquals("Corner 2 costs time", state.cornerMarkers[1].mainIssue)
        assertEquals(listOf(17), state.cornerScores.map(CornerScoreUi::cornerNumber))
    }

    @Test
    fun `build keeps sampled ideal line geometry without smoothing it away`() {
        val state = factory.build(
            input = SessionAnalysisTrackCanvasStateInput(
                trackMap = SessionAnalysisTrackMapUi(
                    points = persistentListOf(
                        SessionAnalysisTrackPointUi(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                        SessionAnalysisTrackPointUi(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                        SessionAnalysisTrackPointUi(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                    ),
                    pitPoints = persistentListOf(),
                    idealPoints = persistentListOf(
                        SessionAnalysisTrackPointUi(x = 0f, y = 0f),
                        SessionAnalysisTrackPointUi(x = 20f, y = 10f),
                        SessionAnalysisTrackPointUi(x = 40f, y = -10f),
                        SessionAnalysisTrackPointUi(x = 60f, y = 10f),
                        SessionAnalysisTrackPointUi(x = 80f, y = -10f),
                        SessionAnalysisTrackPointUi(x = 100f, y = 0f),
                    ),
                    minX = 0f,
                    minY = -10f,
                    maxX = 100f,
                    maxY = 10f,
                ),
                authoredTrackMap = null,
                sourceTrackMap = sessionTrackMap(),
                displayTrackMap = sessionTrackMap(),
                visibleSamplesCore = emptyList(),
                referenceSamplesCore = emptyList(),
                sectors = persistentListOf(),
                highlights = persistentListOf(),
                diagnosticSummary = null,
            ),
        )

        assertNotNull(state)
        assertEquals(listOf(0f, 10f, -10f, 10f, -10f, 0f), state.idealLine.map { it.y })
    }

    @Test
    fun `build prefers authored ideal line over merged display ideal line`() {
        val displayTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            idealPoints = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 2f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 2f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 2f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 2f,
        )
        val authoredTrackMap = displayTrackMap.copy(
            idealPoints = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 20f, y = 10f),
                SessionAnalysisTrackMapPoint(x = 40f, y = -10f),
                SessionAnalysisTrackMapPoint(x = 60f, y = 10f),
                SessionAnalysisTrackMapPoint(x = 80f, y = -10f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f),
            ),
            maxY = 10f,
            minY = -10f,
        )

        val state = factory.build(
            input = SessionAnalysisTrackCanvasStateInput(
                trackMap = SessionAnalysisTrackMapUi(
                    points = persistentListOf(
                        SessionAnalysisTrackPointUi(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                        SessionAnalysisTrackPointUi(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                        SessionAnalysisTrackPointUi(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                    ),
                    pitPoints = persistentListOf(),
                    idealPoints = persistentListOf(),
                    minX = 0f,
                    minY = 0f,
                    maxX = 100f,
                    maxY = 2f,
                ),
                authoredTrackMap = authoredTrackMap,
                sourceTrackMap = displayTrackMap,
                displayTrackMap = displayTrackMap,
                visibleSamplesCore = emptyList(),
                referenceSamplesCore = emptyList(),
                sectors = persistentListOf(),
                highlights = persistentListOf(),
                diagnosticSummary = null,
            ),
        )

        assertNotNull(state)
        assertEquals(listOf(0f, 10f, -10f, 10f, -10f, 0f), state.idealLine.map { it.y })
    }

    @Test
    fun `build keeps adjacent geometry corner zones as separate map markers`() {
        val diagnosticSummary = SessionAnalysisDiagnosticSummaryUi(
            cornerScores = persistentListOf(
                CornerScoreUi(
                    cornerNumber = 32,
                    score = 67,
                    trackPosition = 0.33f,
                    mainIssue = "One physical turn was detected multiple times.",
                    timeVsReferenceMs = 142,
                    recommendation = "Treat as one corner marker.",
                    source = SessionAnalysisDiagnosisSource.DrivingStyle.toUi(),
                ),
            ),
        )

        val state = factory.build(
            input = SessionAnalysisTrackCanvasStateInput(
                trackMap = sessionTrackMapUi(),
                authoredTrackMap = null,
                sourceTrackMap = sessionTrackMap(),
                displayTrackMap = sessionTrackMap(),
                visibleSamplesCore = emptyList(),
                referenceSamplesCore = emptyList(),
                sectors = persistentListOf(),
                highlights = persistentListOf(),
                diagnosticSummary = diagnosticSummary,
                cornerZones = listOf(
                    SessionAnalysisCornerZone(
                        cornerNumber = 7,
                        startTrackPosition = 0.28f,
                        endTrackPosition = 0.32f,
                        apexTrackPosition = 0.30f,
                        peakCurvature = 0.018f,
                    ),
                    SessionAnalysisCornerZone(
                        cornerNumber = 8,
                        startTrackPosition = 0.321f,
                        endTrackPosition = 0.35f,
                        apexTrackPosition = 0.335f,
                        peakCurvature = 0.026f,
                    ),
                    SessionAnalysisCornerZone(
                        cornerNumber = 9,
                        startTrackPosition = 0.351f,
                        endTrackPosition = 0.375f,
                        apexTrackPosition = 0.364f,
                        peakCurvature = 0.021f,
                    ),
                ),
            ),
        )

        assertNotNull(state)
        assertEquals(listOf(7, 8, 9), state.cornerMarkers.map(CornerScoreUi::cornerNumber))
        assertEquals(listOf(100, 67, 100), state.cornerMarkers.map(CornerScoreUi::score))
        assertTrue(
            state.cornerMarkers
                .map(CornerScoreUi::markerTrackPosition)
                .zip(listOf(0.29f, 0.3355f, 0.369f))
                .all { (actual, expected) -> kotlin.math.abs(actual - expected) < 0.0005f },
        )
    }

    @Test
    fun `build keeps nearby disconnected corner zones as separate map markers`() {
        val state = factory.build(
            input = SessionAnalysisTrackCanvasStateInput(
                trackMap = sessionTrackMapUi(),
                authoredTrackMap = null,
                sourceTrackMap = sessionTrackMap(),
                displayTrackMap = sessionTrackMap(),
                visibleSamplesCore = emptyList(),
                referenceSamplesCore = emptyList(),
                sectors = persistentListOf(),
                highlights = persistentListOf(),
                diagnosticSummary = null,
                cornerZones = listOf(
                    SessionAnalysisCornerZone(
                        cornerNumber = 1,
                        startTrackPosition = 0.100f,
                        endTrackPosition = 0.110f,
                        apexTrackPosition = 0.105f,
                        peakCurvature = 0.018f,
                    ),
                    SessionAnalysisCornerZone(
                        cornerNumber = 2,
                        startTrackPosition = 0.123f,
                        endTrackPosition = 0.133f,
                        apexTrackPosition = 0.128f,
                        peakCurvature = 0.020f,
                    ),
                    SessionAnalysisCornerZone(
                        cornerNumber = 3,
                        startTrackPosition = 0.146f,
                        endTrackPosition = 0.156f,
                        apexTrackPosition = 0.151f,
                        peakCurvature = 0.022f,
                    ),
                ),
            ),
        )

        assertNotNull(state)
        assertEquals(listOf(1, 2, 3), state.cornerMarkers.map(CornerScoreUi::cornerNumber))
    }

    @Test
    fun `build ignores detached diagnostic corners when geometry zones exist`() {
        val diagnosticSummary = SessionAnalysisDiagnosticSummaryUi(
            cornerScores = persistentListOf(
                CornerScoreUi(
                    cornerNumber = 5,
                    score = 61,
                    trackPosition = 0.72f,
                    mainIssue = "Telemetry-only corner should not create a map marker.",
                    timeVsReferenceMs = 91,
                    recommendation = "Keep physical turn count stable.",
                    source = SessionAnalysisDiagnosisSource.DrivingStyle.toUi(),
                ),
            ),
        )

        val state = factory.build(
            input = SessionAnalysisTrackCanvasStateInput(
                trackMap = sessionTrackMapUi(),
                authoredTrackMap = null,
                sourceTrackMap = sessionTrackMap(),
                displayTrackMap = sessionTrackMap(),
                visibleSamplesCore = emptyList(),
                referenceSamplesCore = emptyList(),
                sectors = persistentListOf(),
                highlights = persistentListOf(),
                diagnosticSummary = diagnosticSummary,
                cornerZones = listOf(
                    SessionAnalysisCornerZone(
                        cornerNumber = 1,
                        startTrackPosition = 0.08f,
                        endTrackPosition = 0.16f,
                        apexTrackPosition = 0.12f,
                        peakCurvature = 0.021f,
                    ),
                    SessionAnalysisCornerZone(
                        cornerNumber = 2,
                        startTrackPosition = 0.38f,
                        endTrackPosition = 0.50f,
                        apexTrackPosition = 0.44f,
                        peakCurvature = 0.033f,
                    ),
                ),
            ),
        )

        assertNotNull(state)
        assertEquals(listOf(1, 2), state.cornerMarkers.map(CornerScoreUi::cornerNumber))
    }
}

