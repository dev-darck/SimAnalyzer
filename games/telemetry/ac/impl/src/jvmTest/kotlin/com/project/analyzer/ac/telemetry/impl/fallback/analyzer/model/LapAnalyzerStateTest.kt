package com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LapAnalyzerStateTest {

    @Test
    fun `fallback lap counter starts from zero completed laps`() {
        val state = LapAnalyzerState()

        state.resetSession()

        assertEquals(0, state.completedLapsCount)

        state.syncToStartFinish(
            timestampNs = 1_000_000_000L,
            interpolationFactor = 1f,
        )

        assertEquals(0, state.completedLapsCount)

        state.completeLap(
            timestampNs = 2_000_000_000L,
            interpolationFactor = 1f,
        )

        assertEquals(1, state.completedLapsCount)
    }
}
