package com.analyzer.session.analysis.presentation.builder.lap

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisLapFractionResolverTest {

    @Test
    fun `resolveLapFractions unwraps telemetry progress when lap starts before start finish`() {
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0.78f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.86f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 0.94f),
            SessionAnalysisSample(sampleIndexInLap = 3, trackPosition = 0.99f),
            SessionAnalysisSample(sampleIndexInLap = 4, trackPosition = 0.05f),
            SessionAnalysisSample(sampleIndexInLap = 5, trackPosition = 0.14f),
            SessionAnalysisSample(sampleIndexInLap = 6, trackPosition = 0.22f),
        )

        val resolution = resolveLapFractionResolution(samples = samples, trackMap = null)
        val fractions = resolution.fractions

        assertEquals(0f, fractions.first(), 0.0001f)
        assertEquals(1f, fractions.last(), 0.0001f)
        assertTrue(fractions.zipWithNext().all { (start, end) -> end >= start })
        assertTrue(fractions[4] > fractions[3])
        assertEquals(fractions[4], resolution.trailStartFraction ?: -1f, 0.0001f)
    }

    @Test
    fun `resolveLapFractions keeps monotonic telemetry fractions when lap does not wrap`() {
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0.04f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.18f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 0.52f),
            SessionAnalysisSample(sampleIndexInLap = 3, trackPosition = 0.91f),
        )

        val resolution = resolveLapFractionResolution(samples = samples, trackMap = null)
        val fractions = resolution.fractions

        assertEquals(0.04f, fractions[0], 0.0001f)
        assertEquals(0.18f, fractions[1], 0.0001f)
        assertEquals(0.52f, fractions[2], 0.0001f)
        assertEquals(0.91f, fractions[3], 0.0001f)
        assertEquals(null, resolution.trailStartFraction)
    }

    @Test
    fun `resolveLapFractions unwraps geometry progress when telemetry track position is missing`() {
        val trackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 20f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 30f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 40f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 50f,
            maxY = 0f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackX = 38f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackX = 45f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackX = 49f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 3, trackX = 2f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 4, trackX = 9f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 5, trackX = 17f, trackY = 0f),
        )

        val resolution = resolveLapFractionResolution(samples = samples, trackMap = trackMap)
        val fractions = resolution.fractions

        assertEquals(0f, fractions.first(), 0.0001f)
        assertEquals(1f, fractions.last(), 0.0001f)
        assertTrue(fractions.zipWithNext().all { (start, end) -> end >= start })
        assertTrue(fractions[3] >= fractions[2])
        assertEquals(fractions[3], resolution.trailStartFraction ?: -1f, 0.0001f)
    }

    @Test
    fun `resolveTraceLapFractions prefers geometry over noisy telemetry track position`() {
        val trackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 25f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 75f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 0f,
        )
        val samples = listOf(
            SessionAnalysisSample(sampleIndexInLap = 0, trackPosition = 0f, trackX = 0f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 1, trackPosition = 0.72f, trackX = 25f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 2, trackPosition = 0.18f, trackX = 50f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 3, trackPosition = 0.91f, trackX = 75f, trackY = 0f),
            SessionAnalysisSample(sampleIndexInLap = 4, trackPosition = 1f, trackX = 100f, trackY = 0f),
        )

        val fractions = resolveTraceLapFractions(samples = samples, trackMap = trackMap)

        assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f, 1f), fractions)
    }
}

