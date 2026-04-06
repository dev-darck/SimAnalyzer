package com.analyzer.session.analysis.presentation.builder.track

import com.analyzer.session.analysis.presentation.builder.track.trace.SessionAnalysisTrackCanvasTraceBuilder
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisTrackCanvasTraceBuilderTest {

    private val traceBuilder = SessionAnalysisTrackCanvasTraceBuilder()

    @Test
    fun `buildProjectedTrackTrace keeps recorded coordinates when source and display track map match`() {
        val trackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 1f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.5f, trackX = 50f, trackY = 60f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 1f, trackX = 100f, trackY = 0f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = trackMap,
            displayTrackMap = trackMap,
            maxPoints = 16,
        )

        assertEquals(3, trace.size)
        assertEquals(50f, trace[1].x, 0.001f)
        assertEquals(60f, trace[1].y, 0.001f)
    }

    @Test
    fun `buildProjectedTrackTrace keeps raw coordinates for self intersecting display track`() {
        val trackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 100f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 100f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 100f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.5f, trackX = 50f, trackY = 50f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 1f, trackX = 100f, trackY = 0f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = trackMap,
            displayTrackMap = trackMap,
            maxPoints = 16,
        )

        assertEquals(3, trace.size)
        assertEquals(50f, trace[1].x, 0.001f)
        assertEquals(50f, trace[1].y, 0.001f)
    }

    @Test
    fun `buildProjectedTrackTrace preserves raw telemetry zig zag without smoothing it into center line`() {
        val trackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 20f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 30f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 40f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 60f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = -8f,
            maxX = 60f,
            maxY = 8f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 1f / 6f, trackX = 10f, trackY = 8f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 2f / 6f, trackX = 20f, trackY = -8f),
            SessionAnalysisSample(sampleIndexInLap = 3, trackPosition = 3f / 6f, trackX = 30f, trackY = 8f),
            SessionAnalysisSample(sampleIndexInLap = 4, trackPosition = 4f / 6f, trackX = 40f, trackY = -8f),
            SessionAnalysisSample(sampleIndexInLap = 5, trackPosition = 5f / 6f, trackX = 50f, trackY = 8f),
            SessionAnalysisSample(sampleIndexInLap = 6, trackPosition = 1f, trackX = 60f, trackY = 0f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = trackMap,
            displayTrackMap = trackMap,
            maxPoints = 32,
        )

        assertEquals(7, trace.size)
        assertEquals(0f, trace.first().y, 0.001f)
        assertEquals(0f, trace.last().y, 0.001f)
        assertEquals(8f, trace.drop(1).dropLast(1).maxOf { kotlin.math.abs(it.y) }, 0.001f)
    }

    @Test
    fun `buildProjectedTrackTrace corrects impossible local backtrack in recorded telemetry`() {
        val trackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 20f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 40f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 60f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 80f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = -12f,
            maxX = 100f,
            maxY = 12f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.2f, trackX = 20f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 0.4f, trackX = 40f, trackY = 8f),
            SessionAnalysisSample(sampleIndexInLap = 3, trackPosition = 0.6f, trackX = 32f, trackY = -9f),
            SessionAnalysisSample(sampleIndexInLap = 4, trackPosition = 0.8f, trackX = 80f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 5, trackPosition = 1f, trackX = 100f, trackY = 0f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = trackMap,
            displayTrackMap = trackMap,
            maxPoints = 32,
        )

        assertEquals(6, trace.size)
        assertTrue(trace.zipWithNext().all { (first, second) -> second.x >= first.x - 0.001f })
        assertTrue(kotlin.math.abs(trace[3].y) < 5f)
    }

    @Test
    fun `buildProjectedTrackTrace smooths minor jitter on otherwise straight telemetry`() {
        val trackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 20f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 30f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 40f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = -4f,
            maxX = 50f,
            maxY = 4f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.2f, trackX = 10f, trackY = 0.10f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 0.4f, trackX = 20f, trackY = -0.38f),
            SessionAnalysisSample(sampleIndexInLap = 3, trackPosition = 0.6f, trackX = 30f, trackY = 0.42f),
            SessionAnalysisSample(sampleIndexInLap = 4, trackPosition = 0.8f, trackX = 40f, trackY = -0.12f),
            SessionAnalysisSample(sampleIndexInLap = 5, trackPosition = 1f, trackX = 50f, trackY = 0f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = trackMap,
            displayTrackMap = trackMap,
            maxPoints = 32,
        )

        assertEquals(6, trace.size)
        assertTrue(trace.maxOf { kotlin.math.abs(it.y) } < 0.30f)
    }

    @Test
    fun `buildProjectedTrackTrace samples display fallback by path distance instead of point index`() {
        val trackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 60f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 80f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 90f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 90f, y = 40f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 90f, y = 80f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 90f,
            maxY = 80f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.5f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 1f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = trackMap,
            displayTrackMap = trackMap,
            maxPoints = 8,
        )

        assertEquals(3, trace.size)
        assertEquals(81.46f, trace[1].x, 0.2f)
        assertEquals(1.46f, trace[1].y, 0.2f)
    }

    @Test
    fun `buildProjectedTrackTrace projects recorded telemetry from source map to display map`() {
        val sourceTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 1f,
        )
        val displayTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 10f,
            maxX = 100f,
            maxY = 11f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 3f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.5f, trackX = 50f, trackY = 6f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 1f, trackX = 100f, trackY = 3f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
            maxPoints = 16,
        )

        assertEquals(3, trace.size)
        assertEquals(16f, trace[1].y, 0.001f)
    }

    @Test
    fun `buildProjectedTrackTrace keeps raw telemetry when display map is already aligned`() {
        val sourceTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 25f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 75f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 1f,
        )
        val displayTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0.35f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 25f, y = 0.18f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = -0.14f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 75f, y = 0.22f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0.08f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = -1f,
            maxX = 100f,
            maxY = 1f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 3f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.5f, trackX = 50f, trackY = 6f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 1f, trackX = 100f, trackY = 3f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
            maxPoints = 16,
        )

        assertEquals(3, trace.size)
        assertEquals(listOf(3f, 6f, 3f), trace.map { it.y })
    }

    @Test
    fun `buildProjectedTrackTrace keeps raw telemetry when source map is noisy but raw trace already fits display map`() {
        val sourceTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 15f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 30f, y = 7f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 45f, y = -7f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 60f, y = 7f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 75f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 90f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = -8f,
            maxX = 90f,
            maxY = 8f,
        )
        val displayTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 30f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 60f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 90f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = -1f,
            maxX = 90f,
            maxY = 1f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 2f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.16f, trackX = 15f, trackY = 2.5f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 0.33f, trackX = 30f, trackY = 2f),
            SessionAnalysisSample(sampleIndexInLap = 3, trackPosition = 0.5f, trackX = 45f, trackY = 2.3f),
            SessionAnalysisSample(sampleIndexInLap = 4, trackPosition = 0.66f, trackX = 60f, trackY = 2f),
            SessionAnalysisSample(sampleIndexInLap = 5, trackPosition = 0.83f, trackX = 75f, trackY = 2.4f),
            SessionAnalysisSample(sampleIndexInLap = 6, trackPosition = 1f, trackX = 90f, trackY = 2f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
            maxPoints = 32,
        )

        assertEquals(7, trace.size)
        assertEquals(listOf(0f, 15f, 30f, 45f, 60f, 75f, 90f), trace.map { it.x })
        assertTrue(trace.all { point -> point.y in 1.9f..2.6f })
    }

    @Test
    fun `buildProjectedOverlayTrace keeps raw overlay when display map is already aligned`() {
        val sourceTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 25f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 75f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 1f,
        )
        val displayTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0.35f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 25f, y = 0.18f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = -0.14f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 75f, y = 0.22f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0.08f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = -1f,
            maxX = 100f,
            maxY = 1f,
        )
        val overlayPoints = listOf(
            SessionAnalysisTrackMapPoint(x = 0f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 25f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 50f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 75f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 100f, y = 2f),
        )

        val trace = traceBuilder.buildProjectedOverlayTrace(
            overlayPoints = overlayPoints,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
            maxPoints = 32,
        )

        assertEquals(5, trace.size)
        assertTrue(trace.all { point -> kotlin.math.abs(point.y - 2f) <= 0.001f })
    }

    @Test
    fun `buildProjectedOverlayTrace keeps raw overlay when source map is noisy but overlay already fits display map`() {
        val sourceTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 20f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 40f, y = 8f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 60f, y = -8f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 80f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = -8f,
            maxX = 100f,
            maxY = 8f,
        )
        val displayTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = -1f,
            maxX = 100f,
            maxY = 1f,
        )
        val overlayPoints = listOf(
            SessionAnalysisTrackMapPoint(x = 0f, y = 1.5f),
            SessionAnalysisTrackMapPoint(x = 25f, y = 1.5f),
            SessionAnalysisTrackMapPoint(x = 50f, y = 1.5f),
            SessionAnalysisTrackMapPoint(x = 75f, y = 1.5f),
            SessionAnalysisTrackMapPoint(x = 100f, y = 1.5f),
        )

        val trace = traceBuilder.buildProjectedOverlayTrace(
            overlayPoints = overlayPoints,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
            maxPoints = 32,
        )

        assertEquals(5, trace.size)
        assertEquals(listOf(0f, 25f, 50f, 75f, 100f), trace.map { it.x })
        assertTrue(trace.all { point -> kotlin.math.abs(point.y - 1.5f) <= 0.001f })
    }

    @Test
    fun `buildProjectedTrackTrace uses geometry fractions instead of noisy telemetry progress`() {
        val sourceTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 25f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 75f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 1f,
        )
        val displayTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 25f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 75f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 10f,
            maxX = 100f,
            maxY = 11f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 3f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.72f, trackX = 25f, trackY = 3f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 0.18f, trackX = 50f, trackY = 3f),
            SessionAnalysisSample(sampleIndexInLap = 3, trackPosition = 0.91f, trackX = 75f, trackY = 3f),
            SessionAnalysisSample(sampleIndexInLap = 4, trackPosition = 1f, trackX = 100f, trackY = 3f),
        )

        val trace = traceBuilder.buildProjectedTrackTrace(
            samples = samples,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
            maxPoints = 16,
        )

        assertEquals(5, trace.size)
        assertEquals(listOf(0f, 25f, 50f, 75f, 100f), trace.map { it.x })
        assertTrue(trace.all { point -> kotlin.math.abs(point.y - 13f) <= 0.001f })
    }

    @Test
    fun `buildProjectedOverlayTrace filters off track spikes from ideal line`() {
        val sourceTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 1f,
        )
        val overlayPoints = listOf(
            SessionAnalysisTrackMapPoint(x = 0f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 20f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 40f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 60f, y = 26f),
            SessionAnalysisTrackMapPoint(x = 80f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 100f, y = 2f),
        )

        val trace = traceBuilder.buildProjectedOverlayTrace(
            overlayPoints = overlayPoints,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = sourceTrackMap,
            maxPoints = 32,
        )

        assertEquals(5, trace.size)
        assertTrue(trace.all { point -> kotlin.math.abs(point.y - 2f) <= 0.001f })
    }
}

