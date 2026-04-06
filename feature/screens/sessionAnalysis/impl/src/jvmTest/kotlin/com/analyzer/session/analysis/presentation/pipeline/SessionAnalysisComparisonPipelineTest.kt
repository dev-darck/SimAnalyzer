package com.analyzer.session.analysis.presentation.pipeline

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionAnalysisComparisonPipelineTest {

    @Test
    fun `buildComparisonPoints aligns laps by normalized progress instead of raw telemetry position`() {
        val selectedSamples = listOf(
            comparisonSample(
                frameId = 101L,
                timestampNs = 0L,
                sampleIndexInLap = 0,
                trackPosition = 0.22f,
                speedKmh = 120f,
            ),
            comparisonSample(
                frameId = 102L,
                timestampNs = 1_000_000_000L,
                sampleIndexInLap = 1,
                trackPosition = 0.57f,
                speedKmh = 140f,
            ),
            comparisonSample(
                frameId = 103L,
                timestampNs = 2_000_000_000L,
                sampleIndexInLap = 2,
                trackPosition = 0.93f,
                speedKmh = 160f,
            ),
        )
        val referenceSamples = listOf(
            comparisonSample(
                frameId = 201L,
                timestampNs = 0L,
                lapNumber = 2,
                sampleIndexInLap = 0,
                trackPosition = 0.08f,
                speedKmh = 90f,
            ),
            comparisonSample(
                frameId = 202L,
                timestampNs = 2_000_000_000L,
                lapNumber = 2,
                sampleIndexInLap = 1,
                trackPosition = 0.43f,
                speedKmh = 110f,
            ),
            comparisonSample(
                frameId = 203L,
                timestampNs = 4_000_000_000L,
                lapNumber = 2,
                sampleIndexInLap = 2,
                trackPosition = 0.81f,
                speedKmh = 130f,
            ),
        )

        val comparisonPoints = buildComparisonPoints(
            selectedSamples = selectedSamples,
            referenceSamples = referenceSamples,
            trackMap = null,
        )

        assertTrue(comparisonPoints.isNotEmpty())
        assertEquals(0f, comparisonPoints.first().fraction, 0.0001f)
        assertEquals(1f, comparisonPoints.last().fraction, 0.0001f)
        assertEquals(90f, comparisonPoints.first().referenceSpeedKmh ?: error("Missing first reference speed"), 0.0001f)
        assertEquals(130f, comparisonPoints.last().referenceSpeedKmh ?: error("Missing last reference speed"), 0.0001f)
        assertEquals(0, comparisonPoints.first().referenceElapsedMs)
        assertEquals(4_000, comparisonPoints.last().referenceElapsedMs)
    }

    @Test
    fun `buildComparisonPoints keeps reference values null when reference lap is unavailable`() {
        val selectedSamples = listOf(
            comparisonSample(
                frameId = 101L,
                timestampNs = 0L,
                sampleIndexInLap = 0,
                trackPosition = 0.12f,
                speedKmh = 120f,
            ),
            comparisonSample(
                frameId = 102L,
                timestampNs = 1_000_000_000L,
                sampleIndexInLap = 1,
                trackPosition = 0.48f,
                speedKmh = 132f,
            ),
            comparisonSample(
                frameId = 103L,
                timestampNs = 2_000_000_000L,
                sampleIndexInLap = 2,
                trackPosition = 0.88f,
                speedKmh = 144f,
            ),
        )

        val comparisonPoints = buildComparisonPoints(
            selectedSamples = selectedSamples,
            referenceSamples = emptyList(),
            trackMap = null,
        )

        assertTrue(comparisonPoints.isNotEmpty())
        assertNull(comparisonPoints.first().referenceSpeedKmh)
        assertNull(comparisonPoints.first().referenceElapsedMs)
        assertNull(comparisonPoints.first().deltaMs)
    }
}

private fun comparisonSample(
    frameId: Long,
    timestampNs: Long,
    sampleIndexInLap: Int,
    trackPosition: Float,
    speedKmh: Float,
    lapNumber: Int = 1,
): SessionAnalysisSample = SessionAnalysisSample(
    frameId = frameId,
    timestampNs = timestampNs,
    lapNumber = lapNumber,
    sampleIndexInLap = sampleIndexInLap,
    trackPosition = trackPosition,
    speedKmh = speedKmh,
)

