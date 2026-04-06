package com.analyzer.session.analysis.presentation.builder.lap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionAnalysisReferenceLapResolverTest {

    @Test
    fun `findBest skips incomplete-sector short lap even when lap is marked complete`() {
        val candidateLap = buildLap(
            lapNumber = 6,
            durationMs = 90_250,
            sectors = listOf(0, 1, 2),
            pathLengthMeters = 4_280f,
        )
        val badLap = buildLap(
            lapNumber = 8,
            durationMs = 88_000,
            sectors = listOf(0),
            pathLengthMeters = 360f,
        )

        val resolved = findBestReferenceLap(
            segmentLaps = listOf(candidateLap.first, badLap.first),
            segmentSamples = candidateLap.second + badLap.second,
        )

        assertEquals(6, resolved?.lapNumber)
    }

    @Test
    fun `resolve rejects custom reference lap that does not cover all sectors`() {
        val fastestLap = buildLap(
            lapNumber = 5,
            durationMs = 89_900,
            sectors = listOf(0, 1, 2),
            pathLengthMeters = 4_300f,
        )
        val selectedLap = buildLap(
            lapNumber = 6,
            durationMs = 90_250,
            sectors = listOf(0, 1, 2),
            pathLengthMeters = 4_280f,
        )
        val invalidReferenceLap = buildLap(
            lapNumber = 8,
            durationMs = 88_000,
            sectors = listOf(0),
            pathLengthMeters = 360f,
        )

        val resolved = resolveReferenceLap(
            segmentLaps = listOf(fastestLap.first, selectedLap.first, invalidReferenceLap.first),
            segmentSamples = fastestLap.second + selectedLap.second + invalidReferenceLap.second,
            selectedReferenceLapNumber = 8,
            selectedLapNumber = 6,
        )

        assertEquals(5, resolved?.lapNumber)
    }

    @Test
    fun `findBest returns null when no lap satisfies reference requirements`() {
        val onlyShortLap = buildLap(
            lapNumber = 8,
            durationMs = 88_000,
            sectors = listOf(0),
            pathLengthMeters = 360f,
        )

        val resolved = findBestReferenceLap(
            segmentLaps = listOf(onlyShortLap.first),
            segmentSamples = onlyShortLap.second,
        )

        assertNull(resolved)
    }
}
